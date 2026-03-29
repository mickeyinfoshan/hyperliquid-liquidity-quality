package ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine.datatype;

import java.util.List;

public record WindowResult(
        long windowStart,
        long windowEnd,
        long priceLevels,
        long priceRangeTicks,
        double priceRangeAbs,
        double impactBps,
        double impactDollar,
        double totalVolume,
        int tickCount,
        double vwap,
        double highPrice,
        double lowPrice,
        double buyVolume,
        double sellVolume,
        double delta,
        double deltaRatio,
        QualityGrade qualityGrade,
        List<Double> uniquePrices
) {
    public WindowResult {
        uniquePrices = uniquePrices == null ? List.of() : List.copyOf(uniquePrices);
        if (qualityGrade == null) {
            qualityGrade = QualityGrade.fromPriceLevels(priceLevels);
        }
    }
}
