package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype;

public record WsFunding(
        long time,
        String coin,
        String usdc,
        String szi,
        String fundingRate
) {
}
