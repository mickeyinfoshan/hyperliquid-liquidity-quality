package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

public record WsBasicOrder(
        String coin,
        String side,
        String limitPx,
        String sz,
        long oid,
        long timestamp,
        String orderType
) {
}
