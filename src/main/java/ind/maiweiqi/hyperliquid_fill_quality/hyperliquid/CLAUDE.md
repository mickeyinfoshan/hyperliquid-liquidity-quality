# hyperliquid

WebSocket client for real-time data ingestion from the Hyperliquid exchange.

## Key Classes

- **HyperliquidWebSocketClient** — Spring `@Component` implementing `SmartLifecycle` and `TextWebSocketHandler`. Features: automatic reconnection with exponential backoff (5s–60s), ping/pong heartbeat (30s interval), session management. Uses a scheduled thread pool for reconnect and heartbeat tasks.
- **HyperliquidWebSocketProperties** — Spring `@ConfigurationProperties(prefix = "hyperliquid.ws")`. Defaults: URL `wss://api.hyperliquid.xyz/ws`, reconnect delay 5s, max delay 60s, ping interval 30s.

## Sub-packages

- `datatype/` — WebSocket protocol data structures: subscriptions, messages, and exchange event records.

## Configuration

Properties are in `application.yaml` under the `hyperliquid.ws` prefix.
