# Hyperliquid Fill Quality

Real-time trade fill quality analysis tool for the [Hyperliquid](https://hyperliquid.xyz) exchange. Connects via WebSocket, aggregates trades into time windows, and computes quality metrics including price dispersion, market impact, VWAP, and volume delta.

## Features

- **Real-time ingestion** — WebSocket client with automatic reconnection and exponential backoff
- **Time-windowed analysis** — configurable window duration (default 1s)
- **Fill quality metrics** — price dispersion, impact cost (bps + dollar), VWAP, buy/sell delta
- **Quality grading** — EXCELLENT / GOOD / FAIR / POOR / VERY_POOR based on price level thresholds
- **Rolling statistics** — 30-window circular buffer tracking averages, QFR, and MEP
- **Order event tracking** — submitted, filled, cancelled, modified lifecycle events

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 17 |
| Spring Boot | 4.0.5 |
| Build | Maven (wrapper included) |
| WebSocket | Spring WebSocket + Jakarta |
| JSON | Jackson |
| Code generation | Lombok |
| Testing | JUnit 5 |

## Quick Start

### Prerequisites

- Java 17+

### Build & Run

```bash
# Build
./mvnw package -DskipTests

# Run
./mvnw spring-boot:run
```

The application connects to Hyperliquid's WebSocket API and begins streaming trade data for the configured symbol.

## Configuration

Edit `src/main/resources/application.yaml`:

```yaml
hyperliquid:
  ws:
    url: wss://api.hyperliquid.xyz/ws
    reconnect-delay-ms: 5000
    max-reconnect-delay-ms: 60000
    ping-interval-ms: 30000

fill-quality:
  symbol: BTC
  tick-size: 0.1
  ref-price: 87000.0
  contract-multiplier: 1
  window-duration-ms: 1000
```

| Property | Description | Default |
|----------|-------------|---------|
| `hyperliquid.ws.url` | WebSocket endpoint | `wss://api.hyperliquid.xyz/ws` |
| `hyperliquid.ws.reconnect-delay-ms` | Initial reconnect delay | `5000` |
| `hyperliquid.ws.max-reconnect-delay-ms` | Max reconnect delay (exponential backoff) | `60000` |
| `hyperliquid.ws.ping-interval-ms` | Heartbeat interval | `30000` |
| `fill-quality.symbol` | Trading symbol to analyze | `BTC` |
| `fill-quality.tick-size` | Minimum price increment | `0.1` |
| `fill-quality.ref-price` | Reference price for impact calculation | `87000.0` |
| `fill-quality.contract-multiplier` | Volume multiplier | `1` |
| `fill-quality.window-duration-ms` | Analysis window length (ms) | `1000` |

## Architecture

```
WebSocket Feed ──> HyperliquidWebSocketClient ──> FillQualityService ──> FillQualityEngine
                   (auto-reconnect, heartbeat)    (tick conversion,      (window aggregation,
                                                    scheduling)           metrics computation)
                                                                              │
                                                                              ▼
                                                                     WindowProcessedListener
                                                                     FillQualityStats (rolling)
```

**Two packages:**

- `fill_quality_engine` — Core analysis: accumulates ticks, computes per-window metrics (dispersion, impact, VWAP, delta), maintains rolling statistics
- `hyperliquid` — Exchange integration: WebSocket client, protocol data types, subscription management

## Metrics

| Metric | Formula |
|--------|---------|
| Price Levels | Count of unique prices in window |
| Price Range (ticks) | `(high - low) / tickSize` |
| Impact (bps) | `priceLevels * tickSize / refPrice * 10000` |
| Impact ($) | `priceLevels * tickSize * contractMultiplier` |
| VWAP | `sum(price * volume) / totalVolume` |
| Delta | `buyVolume - sellVolume` |
| QFR | `ordersFilled / ordersSubmitted * 100%` |
| MEP | `(modifications + cancellations * 3) / filled` |
| Sqrt Impact | `spreadCost + factor * dailyVol * sqrt(orderQty / ADTV)` |

### Quality Grades

| Grade | Max Price Levels (bps) |
|-------|----------------------|
| EXCELLENT | <= 1.5 |
| GOOD | <= 3.0 |
| FAIR | <= 5.0 |
| POOR | <= 8.0 |
| VERY_POOR | > 8.0 |

## Testing

```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=FillQualityEngineTest

# Run a single test method
./mvnw test -Dtest=FillQualityEngineTest#processWindow_singleTick
```
