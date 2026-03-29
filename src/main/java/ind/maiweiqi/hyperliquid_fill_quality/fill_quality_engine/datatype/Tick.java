package ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine.datatype;

public record Tick(
        double price,
        long volume,
        long timestamp,
        Side side,
        String orderId
) {
    public Tick {
        if (volume <= 0) {
            throw new IllegalArgumentException("volume must be positive");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp must be positive");
        }
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }
        if (orderId == null) {
            orderId = "";
        }
    }

    public Tick(double price, long volume, long timestamp, Side side) {
        this(price, volume, timestamp, side, "");
    }
}
