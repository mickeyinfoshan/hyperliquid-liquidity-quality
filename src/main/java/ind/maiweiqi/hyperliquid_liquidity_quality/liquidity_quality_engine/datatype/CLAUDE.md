# liquidity_quality_engine.datatype

Immutable data structures and enums for the liquidity quality engine.

## Enums

- **Side** — `BUY`, `SELL`
- **QualityGrade** — `EXCELLENT`, `GOOD`, `FAIR`, `POOR`, `VERY_POOR` with bps thresholds

## Records

- **Tick** — Individual trade tick: `price`, `volume`, `timestamp`, `side`. Has validation in compact constructor.
- **ProductConfig** — Product configuration: `symbol`, `tickSize`, `refPrice`, `contractMultiplier`. Used for impact calculations.
- **WindowResult** — Complete window analysis output (18 fields): price levels, dispersion, impact (bps + dollar), VWAP, volume breakdown, quality grade. Defensive-copies the price list.
