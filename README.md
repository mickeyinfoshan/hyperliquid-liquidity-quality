# Hyperliquid Liquidity Quality

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> **Tick-level liquidity X-ray for Hyperliquid** — plug in, stream trades, get institutional-grade quality scores in real time.

Every millisecond, Hyperliquid's order book tells a story: who's providing liquidity, how thick the book really is, and whether you're about to get filled at a fair price or eat three ticks of slippage. This tool **listens to every trade via WebSocket**, slices the stream into sub-second windows, and spits out the metrics that matter — **price dispersion, market impact (bps & dollar), VWAP, delta, and a letter grade** so you know at a glance whether liquidity is EXCELLENT or dumpster-fire VERY_POOR.

---

## Methodology

The analytical framework behind this project is derived from **[Reassessing Liquidity: Beyond Order Book Depth](https://www.cmegroup.com/articles/2025/reassessing-liquidity-beyond-order-book-depth.html)**, a whitepaper published by **CME Group** in 2025.

The paper argues that traditional order book depth is a poor proxy for true market liquidity. Instead, CME proposes a more comprehensive evaluation framework built on the following pillars:

- **Price Dispersion** — measures how scattered execution prices are within a time window, revealing the real "thickness" of liquidity
- **Market Impact Cost** — quantifies the actual price impact of trades in both **bps and USD**, making hidden execution costs visible
- **VWAP (Volume-Weighted Average Price)** — the volume-weighted average execution price, the gold standard for assessing fill quality
- **Quote Fill Rate (QFR)** — the probability that passive orders get executed, distinguishing real liquidity from phantom quotes
- **Market Efficiency Penalty (MEP)** — penalizes modifications and cancellations to quantify "fake liquidity" contributed by fleeting orders
- **Sqrt Impact Model** — a square-root market impact model based on ADTV (Average Daily Trading Volume) for pre-trade cost estimation on large orders

This tool **transplants the CME methodology from traditional futures markets to the Hyperliquid perpetual DEX**, computing these metrics in real time at sub-second resolution — bringing institutional-grade liquidity insight to on-chain traders.

---

## Why This Exists

| Problem | Solution |
|---------|----------|
| CEX dashboards show lagging, averaged spreads | Sub-second sliding windows capture **real** microstructure |
| Impact cost is invisible until you trade | Pre-trade impact estimation in **bps + USD** |
| No way to score liquidity over time | 30-window rolling stats with **QFR** (Quote Fill Rate) and **MEP** (Market Efficiency Penalty) |
| WebSocket drops silently | Auto-reconnect with **exponential backoff**, zero data loss guarantee on reconnection |

---

## Key Features

- **Sub-second resolution** — configurable time windows (default 1 s), capturing microstructure that minute-level tools miss entirely
- **Institutional-grade metrics** — Sqrt Impact Model, VWAP, buy/sell delta, price dispersion — the same toolkit prop desks use
- **5-tier quality grading** — EXCELLENT / GOOD / FAIR / POOR / VERY_POOR, automatically computed per window
- **Rolling statistics** — 30-window circular buffer for trend detection; spot liquidity regime changes as they happen
- **Battle-tested connectivity** — WebSocket auto-reconnect with exponential backoff (5 s -> 60 s cap), heartbeat keep-alive
- **Zero dependencies beyond Spring** — no Kafka, no Redis, no database. One JVM, one config file, done

---

## 30-Second Quick Start

```bash
# 1. Clone & build
git clone https://github.com/yourname/hyperliquid-liquidity-quality.git
cd hyperliquid-liquidity-quality
./mvnw package -DskipTests

# 2. Run — starts streaming BTC trades immediately
./mvnw spring-boot:run
```

That's it. The app connects to `wss://api.hyperliquid.xyz/ws`, subscribes to BTC trades, and starts printing window-by-window liquidity scores to stdout.

> **Prerequisite:** Java 17+. No Maven install needed — wrapper included.

---

## Architecture

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                    Spring Boot Application              │
                    │                                                         │
  Hyperliquid WS    │  ┌──────────────────┐    ┌────────────────────────┐    │
  (trades stream)───┼─>│  WebSocketClient  │───>│ LiquidityQualityService│    │
                    │  │  - auto reconnect │    │  - tick conversion     │    │
                    │  │  - heartbeat      │    │  - window scheduling   │    │
                    │  │  - backoff 5-60s  │    └───────────┬────────────┘    │
                    │  └──────────────────┘                │                  │
                    │                                      ▼                  │
                    │                         ┌────────────────────────┐      │
                    │                         │ LiquidityQualityEngine │      │
                    │                         │  - tick accumulation   │      │
                    │                         │  - window aggregation  │      │
                    │                         │  - metrics computation │      │
                    │                         └───────────┬────────────┘      │
                    │                                     │                   │
                    │                          ┌──────────┴──────────┐        │
                    │                          ▼                     ▼        │
                    │                 WindowProcessed      LiquidityQuality   │
                    │                   Listener              Stats           │
                    │                 (per-window)       (30-window rolling)  │
                    └─────────────────────────────────────────────────────────┘
```

**Two packages, clear separation of concerns:**

| Package | Responsibility |
|---------|---------------|
| `liquidity_quality_engine` | Core analysis — tick accumulation, window metrics (dispersion, impact, VWAP, delta), rolling statistics |
| `hyperliquid` | Exchange integration — WebSocket lifecycle, protocol types, subscription management |

---

## Metrics Reference

### Per-Window Metrics

| Metric | Formula | What It Tells You |
|--------|---------|-------------------|
| Price Levels | Count of unique prices | Book depth consumed in this window |
| Price Range | `(high - low) / tickSize` | Spread volatility in ticks |
| Impact (bps) | `priceLevels * tickSize / refPrice * 10000` | Cost of walking the book |
| Impact ($) | `priceLevels * tickSize * contractMultiplier` | Dollar cost of walking the book |
| VWAP | `sum(price * volume) / totalVolume` | Fair execution price |
| Delta | `buyVolume - sellVolume` | Aggressor imbalance |
| Sqrt Impact | `spreadCost + factor * dailyVol * sqrt(orderQty / ADTV)` | Pre-trade impact estimation |

### Rolling Statistics (30-Window)

| Metric | Formula | What It Tells You |
|--------|---------|-------------------|
| QFR | `ordersFilled / ordersSubmitted * 100%` | Quote Fill Rate — how often passive orders get hit |
| MEP | `(modifications + cancellations * 3) / filled` | Market Efficiency Penalty — higher = more toxic flow |

### Quality Grades

| Grade | Impact (bps) | Interpretation |
|-------|:------------:|----------------|
| **EXCELLENT** | <= 1.5 | Razor-tight, institutional-quality liquidity |
| **GOOD** | <= 3.0 | Healthy book, minimal slippage |
| **FAIR** | <= 5.0 | Acceptable for most order sizes |
| **POOR** | <= 8.0 | Thin book, consider splitting orders |
| **VERY_POOR** | > 8.0 | Danger zone — expect significant slippage |

---

## Configuration

All settings live in `src/main/resources/application.yaml`:

```yaml
hyperliquid:
  ws:
    url: wss://api.hyperliquid.xyz/ws
    reconnect-delay-ms: 5000
    max-reconnect-delay-ms: 60000
    ping-interval-ms: 30000

liquidity-quality:
  symbol: BTC
  tick-size: 0.1
  ref-price: 87000.0
  contract-multiplier: 1
  window-duration-ms: 1000
```

<details>
<summary>Full property reference</summary>

| Property | Description | Default |
|----------|-------------|---------|
| `hyperliquid.ws.url` | WebSocket endpoint | `wss://api.hyperliquid.xyz/ws` |
| `hyperliquid.ws.reconnect-delay-ms` | Initial reconnect delay | `5000` |
| `hyperliquid.ws.max-reconnect-delay-ms` | Max backoff cap | `60000` |
| `hyperliquid.ws.ping-interval-ms` | Heartbeat interval | `30000` |
| `liquidity-quality.symbol` | Trading symbol | `BTC` |
| `liquidity-quality.tick-size` | Minimum price increment | `0.1` |
| `liquidity-quality.ref-price` | Reference price for impact calc | `87000.0` |
| `liquidity-quality.contract-multiplier` | Volume multiplier | `1` |
| `liquidity-quality.window-duration-ms` | Window length (ms) | `1000` |

</details>

---

## Tech Stack

| | |
|---|---|
| **Runtime** | Java 17, Spring Boot 4.0.5 |
| **Transport** | Spring WebSocket + Jakarta WebSocket API |
| **Serialization** | Jackson |
| **Code Gen** | Lombok |
| **Build** | Maven wrapper (zero install) |
| **Test** | JUnit 5 (14 unit tests covering edge cases, rolling stats, defensive copying) |

---

## Testing

```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=LiquidityQualityEngineTest

# Run a single test method
./mvnw test -Dtest=LiquidityQualityEngineTest#processWindow_singleTick
```

---

## License

MIT
