package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype;

public record WsOrder(
        WsBasicOrder order,
        String status,
        long statusTimestamp
) {
}
