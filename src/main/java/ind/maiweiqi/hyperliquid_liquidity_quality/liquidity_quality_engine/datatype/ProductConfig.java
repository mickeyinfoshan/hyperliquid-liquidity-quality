package ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine.datatype;

public record ProductConfig(
        String symbol,
        double tickSize,
        long contractMultiplier
) {
    public ProductConfig {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be positive");
        }
        if (contractMultiplier <= 0) {
            throw new IllegalArgumentException("contractMultiplier must be positive");
        }
    }
}
