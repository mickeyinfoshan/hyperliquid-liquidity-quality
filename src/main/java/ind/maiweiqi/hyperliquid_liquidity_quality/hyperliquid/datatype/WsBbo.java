package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype;

public record WsBbo(
        String coin,
        long time,
        WsBboData bbo
) {
}
