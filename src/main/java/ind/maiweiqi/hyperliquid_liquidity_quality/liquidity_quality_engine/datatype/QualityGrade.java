package ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine.datatype;

public enum QualityGrade {

    EXCELLENT(1.5),
    GOOD(3.0),
    FAIR(5.0),
    POOR(8.0),
    VERY_POOR(Double.MAX_VALUE);

    private final double maxPriceLevels;

    QualityGrade(double maxPriceLevels) {
        this.maxPriceLevels = maxPriceLevels;
    }

    public static QualityGrade fromPriceLevels(double priceLevels) {
        for (QualityGrade grade : values()) {
            if (priceLevels <= grade.maxPriceLevels) {
                return grade;
            }
        }
        return VERY_POOR;
    }

}
