package ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine.datatype;

public enum QualityGrade {

    EXCELLENT(1.5),
    GOOD(3.0),
    FAIR(5.0),
    POOR(8.0),
    VERY_POOR(Double.MAX_VALUE);

    private final double maxImpactBps;

    QualityGrade(double maxImpactBps) {
        this.maxImpactBps = maxImpactBps;
    }

    public static QualityGrade fromImpactBps(double impactBps) {
        for (QualityGrade grade : values()) {
            if (impactBps <= grade.maxImpactBps) {
                return grade;
            }
        }
        return VERY_POOR;
    }

}
