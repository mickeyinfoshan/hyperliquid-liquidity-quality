# hyperliquid.datatype

WebSocket protocol data structures for Hyperliquid exchange integration.

## Enums

- **Channel** — 23 WebSocket channel types (`ALL_MIDS`, `L2_BOOK`, `TRADES`, `CANDLE`, `BBO`, `ORDER_UPDATES`, `USER_FILLS`, `USER_FUNDINGS`, etc.). Uses `@JsonValue`/`@JsonCreator` for serialization.
- **CandleInterval** — 14 candle intervals (`M1`..`MO1`) with JSON mapping.

## Protocol Records

- **WsRequest** — RPC wrapper with `subscribe()`/`unsubscribe()` factory methods.
- **Subscription** — Subscription request with 20+ static factory methods for market data (`trades()`, `l2Book()`, `candle()`, `bbo()`) and user data (`orderUpdates()`, `userFills()`, etc.).
- **WsMessage** — Generic message envelope: `channel` + Jackson `JsonNode` data.

## Market Data Records

- **WsTrade** — Trade event: `coin`, `side`, `px`, `sz`, `hash`, `time`, `tid`
- **WsBook** / **WsLevel** — L2 order book snapshot with price levels
- **WsCandle** — OHLCV candle data
- **WsBbo** / **WsBboData** — Best bid/offer prices and sizes

## User Data Records

- **WsOrder** / **WsBasicOrder** — Order updates with status tracking
- **WsFill** / **WsUserFills** — Trade fills with snapshot flag
- **WsFunding** / **WsUserFundings** — Funding payments with snapshot flag
