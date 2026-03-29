package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

public record WsBbo(
        String coin,
        long time,
        WsBboData bbo
) {
}
