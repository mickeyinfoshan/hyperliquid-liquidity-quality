package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

public record WsFill(
        String coin,
        String px,
        String sz,
        String side,
        long time,
        long oid,
        String fee,
        long tid,
        String feeToken
) {
}
