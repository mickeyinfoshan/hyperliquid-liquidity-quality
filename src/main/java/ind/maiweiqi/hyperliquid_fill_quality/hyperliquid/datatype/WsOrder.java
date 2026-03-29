package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

public record WsOrder(
        WsBasicOrder order,
        String status,
        long statusTimestamp
) {
}
