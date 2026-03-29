# liquidity_quality_engine

Core engine for analyzing trade liquidity quality using time-windowed metrics.

## Key Classes

- **LiquidityQualityEngine** — Processes raw `Tick` data into `WindowResult` snapshots. Accumulates ticks, builds sorted price levels via `TreeSet`, computes VWAP, impact (CME formula), and dispersion per window. Notifies registered `WindowProcessedListener`s.
- **LiquidityQualityStats** — Maintains lifetime and rolling statistics using a 30-window circular buffer. Tracks rolling averages for dispersion and impact, max values.
- **WindowProcessedListener** — Functional interface (observer pattern) for window completion events.

## Sub-packages

- `datatype/` — Immutable records (`Tick`, `WindowResult`, `ProductConfig`) and enums (`Side`, `QualityGrade`).

## Design Notes

- All data types are Java records with built-in validation.
- `WindowResult` uses defensive copying for its price list.
- Quality grades: EXCELLENT (<=1.5 bps), GOOD (<=3.0), FAIR (<=5.0), POOR (<=8.0), VERY_POOR (>8.0).
