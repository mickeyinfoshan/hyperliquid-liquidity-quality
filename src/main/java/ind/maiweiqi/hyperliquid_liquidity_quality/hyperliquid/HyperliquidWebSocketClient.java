package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@EnableConfigurationProperties(HyperliquidWebSocketProperties.class)
public class HyperliquidWebSocketClient extends TextWebSocketHandler implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(HyperliquidWebSocketClient.class);
    private static final String PING_MESSAGE = "{\"method\":\"ping\"}";

    private final HyperliquidWebSocketProperties properties;
    private final ObjectMapper objectMapper;
    private final List<WsMessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final List<WsConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hl-ws-scheduler");
        t.setDaemon(true);
        return t;
    });

    private volatile WebSocketSession session;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean intentionalDisconnect = new AtomicBoolean(false);
    private final AtomicLong currentReconnectDelay = new AtomicLong();
    private ScheduledFuture<?> pingTask;
    private ScheduledFuture<?> reconnectTask;

    public HyperliquidWebSocketClient(HyperliquidWebSocketProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.currentReconnectDelay.set(properties.getReconnectDelayMs());
    }

    // --- SmartLifecycle ---

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            connect();
        }
    }

    @Override
    public void stop() {
        disconnect();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; // start last, stop first
    }

    // --- Connection Management ---

    public void connect() {
        intentionalDisconnect.set(false);
        doConnect();
    }

    public void disconnect() {
        intentionalDisconnect.set(true);
        running.set(false);
        cancelPing();
        cancelReconnect();
        closeSession();
    }

    public boolean isConnected() {
        WebSocketSession s = session;
        return s != null && s.isOpen();
    }

    public void send(String text) throws IOException {
        WebSocketSession s = session;
        if (s == null || !s.isOpen()) {
            throw new IOException("WebSocket is not connected");
        }
        s.sendMessage(new TextMessage(text));
    }

    // --- WebSocketHandler callbacks ---

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        currentReconnectDelay.set(properties.getReconnectDelayMs());
        startPing();
        log.info("WebSocket connected to {}", properties.getUrl());
        for (WsConnectionListener listener : connectionListeners) {
            try {
                listener.onConnected();
            } catch (Exception e) {
                log.warn("Connection listener error", e);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("Received: {}", payload);
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode channelNode = root.get("channel");
            JsonNode dataNode = root.get("data");
            if (channelNode != null && dataNode != null) {
                WsMessage wsMessage = new WsMessage(channelNode.asText(), dataNode);
                for (WsMessageListener listener : messageListeners) {
                    try {
                        listener.onMessage(wsMessage);
                    } catch (Exception e) {
                        log.warn("Message listener error on channel {}", wsMessage.channel(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        this.session = null;
        cancelPing();
        log.info("WebSocket closed: {}", status);
        for (WsConnectionListener listener : connectionListeners) {
            try {
                listener.onDisconnected();
            } catch (Exception e) {
                log.warn("Disconnection listener error", e);
            }
        }
        scheduleReconnect();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error", exception);
        closeSession();
        scheduleReconnect();
    }

    // --- Listeners ---

    public void addMessageListener(WsMessageListener listener) {
        messageListeners.add(listener);
    }

    public void addConnectionListener(WsConnectionListener listener) {
        connectionListeners.add(listener);
    }

    // --- Internal ---

    private void doConnect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.setDefaultMaxTextMessageBufferSize(1024 * 1024);
            container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
            StandardWebSocketClient client = new StandardWebSocketClient(container);
            client.execute(this, new WebSocketHttpHeaders(), URI.create(properties.getUrl()));
            log.info("Connecting to {}...", properties.getUrl());
        } catch (Exception e) {
            log.error("Failed to initiate WebSocket connection", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (intentionalDisconnect.get()) {
            return;
        }
        cancelReconnect();
        long delay = currentReconnectDelay.get();
        log.info("Scheduling reconnect in {}ms", delay);
        reconnectTask = scheduler.schedule(() -> {
            // exponential backoff for next attempt
            long next = Math.min(delay * 2, properties.getMaxReconnectDelayMs());
            currentReconnectDelay.set(next);
            doConnect();
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void startPing() {
        cancelPing();
        pingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                send(PING_MESSAGE);
            } catch (IOException e) {
                log.debug("Ping failed: {}", e.getMessage());
            }
        }, properties.getPingIntervalMs(), properties.getPingIntervalMs(), TimeUnit.MILLISECONDS);
    }

    private void cancelPing() {
        if (pingTask != null) {
            pingTask.cancel(false);
            pingTask = null;
        }
    }

    private void cancelReconnect() {
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
    }

    private void closeSession() {
        WebSocketSession s = session;
        session = null;
        if (s != null && s.isOpen()) {
            try {
                s.close();
            } catch (IOException e) {
                log.debug("Error closing session: {}", e.getMessage());
            }
        }
    }
}
