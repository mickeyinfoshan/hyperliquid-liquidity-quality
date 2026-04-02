package ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine.datatype.ProductConfig;
import ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine.datatype.Side;
import ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine.datatype.Tick;
import ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine.datatype.WindowResult;
import ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.HyperliquidWebSocketClient;
import ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.WsConnectionListener;
import ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.WsMessageListener;
import ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype.Subscription;
import ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype.WsMessage;
import ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype.WsRequest;
import ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype.WsTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

@Component
@EnableConfigurationProperties(LiquidityQualityProperties.class)
public class LiquidityQualityService implements SmartLifecycle, WsMessageListener, WsConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(LiquidityQualityService.class);
    private static final String TRADES_CHANNEL = "trades";

    private final HyperliquidWebSocketClient wsClient;
    private final LiquidityQualityProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LiquidityQualityEngine engine;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "fq-window-scheduler");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running;
    private ScheduledFuture<?> windowTask;

    public LiquidityQualityService(HyperliquidWebSocketClient wsClient,
                              LiquidityQualityProperties properties) {
        this.wsClient = wsClient;
        this.properties = properties;

        ProductConfig config = new ProductConfig(
                properties.getSymbol(),
                properties.getTickSize(),
                properties.getContractMultiplier()
        );
        this.engine = new LiquidityQualityEngine(config, properties.getWindowDurationMs());

        this.engine.addListener(this::onWindowProcessed);
    }

    // --- SmartLifecycle ---

    @Override
    public void start() {
        wsClient.addMessageListener(this);
        wsClient.addConnectionListener(this);
        startWindowScheduler();
        running = true;
        log.info("LiquidityQualityService started for {} (window={}ms)", properties.getSymbol(), properties.getWindowDurationMs());
    }

    @Override
    public void stop() {
        running = false;
        if (windowTask != null) {
            windowTask.cancel(false);
            windowTask = null;
        }
        scheduler.shutdown();
        log.info("LiquidityQualityService stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1; // start after WS client, stop before it
    }

    // --- WsConnectionListener ---

    @Override
    public void onConnected() {
        subscribe();
    }

    // --- WsMessageListener ---

    @Override
    public void onMessage(WsMessage message) {
        if (!TRADES_CHANNEL.equals(message.channel())) {
            return;
        }
        try {
            List<WsTrade> trades = objectMapper.convertValue(
                    message.data(),
                    new TypeReference<>() {}
            );
            for (WsTrade trade : trades) {
                Tick tick = toTick(trade);
                synchronized (engine) {
                    engine.addTick(tick);
                }
            }
            log.debug("Processed {} trades for {}", trades.size(), properties.getSymbol());
        } catch (Exception e) {
            log.warn("Failed to process trades message", e);
        }
    }

    // --- Internal ---

    private void subscribe() {
        try {
            WsRequest request = WsRequest.subscribe(Subscription.trades(properties.getSymbol()));
            String json = objectMapper.writeValueAsString(request);
            wsClient.send(json);
            log.info("Subscribed to trades for {}", properties.getSymbol());
        } catch (Exception e) {
            log.error("Failed to subscribe to trades for {}", properties.getSymbol(), e);
        }
    }

    private void startWindowScheduler() {
        windowTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                synchronized (engine) {
                    engine.processWindow(now);
                }
            } catch (Exception e) {
                log.warn("Window processing error", e);
            }
        }, properties.getWindowDurationMs(), properties.getWindowDurationMs(), TimeUnit.MILLISECONDS);
    }

    private Tick toTick(WsTrade trade) {
        double price = Double.parseDouble(trade.px());
        double volume = Double.parseDouble(trade.sz()) * properties.getContractMultiplier();
        Side side = "B".equals(trade.side()) ? Side.BUY : Side.SELL;
        return new Tick(price, volume, trade.time(), side);
    }

    private void onWindowProcessed(WindowResult result) {
        log.info("[{}] Window {}: grade={}, ticks={}, impact={}bps, vwap={}, vol={}",
                properties.getSymbol(),
                result.windowEnd(),
                result.qualityGrade(),
                result.tickCount(),
                result.impactBps(),
                result.vwap(),
                String.format("%.4f", result.totalVolume())
        );
    }
}
