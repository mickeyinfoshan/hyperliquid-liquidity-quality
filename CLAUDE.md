# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Hyperliquid liquidity quality analysis tool — a Spring Boot 4.0.5 application (Java 17) that ingests real-time trade data via WebSocket from the Hyperliquid exchange, aggregates trades into time windows, and computes liquidity quality metrics (price dispersion, impact cost, VWAP, delta, quality grades).

**Base package:** `ind.maiweiqi.hyperliquid_liquidity_quality` (note: underscores, not hyphens)

## Build & Run Commands

```bash
# Build (skip tests)
./mvnw package -DskipTests

# Run
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=LiquidityQualityEngineTest

# Run a single test method
./mvnw test -Dtest=LiquidityQualityEngineTest#processWindow_singleTick
```

## Architecture

- **Build:** Maven wrapper (`mvnw`) — no local Maven install required
- **Framework:** Spring Boot 4.0.5 with `spring-boot-starter` + `spring-boot-starter-websocket`
- **Config:** `src/main/resources/application.yaml`
- **Tests:** JUnit 5 via `spring-boot-starter-test`

### Package Structure

```
ind.maiweiqi.hyperliquid_liquidity_quality
├── liquidity_quality_engine/           # Core analysis engine
│   ├── LiquidityQualityEngine          # Accumulates ticks, computes per-window metrics
│   ├── LiquidityQualityService         # Spring service (SmartLifecycle), bridges WS to engine
│   ├── LiquidityQualityStats           # Lifetime + 30-window rolling statistics (circular buffer)
│   ├── LiquidityQualityProperties      # @ConfigurationProperties for liquidity-quality.*
│   ├── WindowProcessedListener    # Observer callback for window results
│   └── datatype/                  # Immutable records + enums
│       ├── Tick, ProductConfig, WindowResult (records)
│       └── Side, QualityGrade (enums)
└── hyperliquid/                   # Exchange WebSocket integration
    ├── HyperliquidWebSocketClient # WebSocket handler (SmartLifecycle), auto-reconnect with backoff
    ├── HyperliquidWebSocketProperties # @ConfigurationProperties for hyperliquid.ws.*
    ├── WsMessageListener          # Message callback interface
    ├── WsConnectionListener       # Connection lifecycle callback
    └── datatype/                  # WebSocket protocol structures
        ├── Channel, CandleInterval (enums)
        ├── Subscription, WsMessage, WsRequest (records)
        └── WsTrade, WsOrder, WsFill, ... (records)
```

### Key Design Patterns

- **Observer:** WindowProcessedListener, WsMessageListener, WsConnectionListener
- **SmartLifecycle ordering:** WS client (phase=MAX_VALUE) starts first, then LiquidityQualityService (phase=MAX_VALUE-1)
- **Thread safety:** `synchronized` on engine instance, `CopyOnWriteArrayList` for listeners, `volatile` session
- **Records:** All data types are immutable records with compact constructors for validation

## Configuration

Properties in `application.yaml`:

| Prefix | Key | Default | Description |
|--------|-----|---------|-------------|
| `hyperliquid.ws` | `url` | `wss://api.hyperliquid.xyz/ws` | WebSocket endpoint |
| | `reconnect-delay-ms` | `5000` | Initial reconnect delay |
| | `max-reconnect-delay-ms` | `60000` | Max reconnect delay (exponential backoff) |
| | `ping-interval-ms` | `30000` | Heartbeat interval |
| `liquidity-quality` | `symbol` | `BTC` | Trading symbol to analyze |
| | `tick-size` | `0.1` | Minimum price increment |

| | `contract-multiplier` | `1` | Volume multiplier |
| | `window-duration-ms` | `1000` | Time window length |

## Conventions

- **Lombok:** Use `@Data`, `@Slf4j`, etc. — prefer Lombok annotations over manual boilerplate
- **Data types:** Java records for immutable value objects; compact constructors for validation
- **Defensive copying:** `WindowResult` copies its price list; follow this pattern for mutable collections in records

## Testing

- `LiquidityQualityEngineTest` — 14 unit tests covering window processing, VWAP, impact, delta, quality grades, rolling stats, input validation, defensive copying
- `HyperliquidLiquidityQualityApplicationTests` — Spring context load test
