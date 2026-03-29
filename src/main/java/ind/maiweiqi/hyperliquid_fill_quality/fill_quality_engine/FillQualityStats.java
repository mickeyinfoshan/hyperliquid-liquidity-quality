package ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine;

import ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine.datatype.WindowResult;
import lombok.Getter;

@Getter
public class FillQualityStats {

    private static final int ROLLING_WINDOW_SIZE = 30;

    // Lifetime counters
    private long totalTicks;
    private double totalVolume;
    private long totalWindows;

    // Rolling statistics
    private double avgDispersion30;
    private double avgImpactBps30;
    private long maxDispersion;
    private double maxImpactBps;

    // Circular buffer for rolling averages
    private final double[] dispersionBuffer = new double[ROLLING_WINDOW_SIZE];
    private final double[] impactBpsBuffer = new double[ROLLING_WINDOW_SIZE];
    private int bufferIndex;
    private int bufferCount;

    public void updateFromWindow(WindowResult result) {
        totalVolume += result.totalVolume();
        totalWindows++;

        // Update circular buffer
        dispersionBuffer[bufferIndex] = result.priceLevels();
        impactBpsBuffer[bufferIndex] = result.impactBps();
        bufferIndex = (bufferIndex + 1) % ROLLING_WINDOW_SIZE;
        if (bufferCount < ROLLING_WINDOW_SIZE) {
            bufferCount++;
        }

        // Rolling averages
        double dispSum = 0, impactSum = 0;
        for (int i = 0; i < bufferCount; i++) {
            dispSum += dispersionBuffer[i];
            impactSum += impactBpsBuffer[i];
        }
        avgDispersion30 = dispSum / bufferCount;
        avgImpactBps30 = impactSum / bufferCount;

        // Max values
        if (result.priceLevels() > maxDispersion) {
            maxDispersion = result.priceLevels();
        }
        if (result.impactBps() > maxImpactBps) {
            maxImpactBps = result.impactBps();
        }
    }

    public void incrementTotalTicks(int count) {
        totalTicks += count;
    }

    public void reset() {
        totalTicks = 0;
        totalVolume = 0;
        totalWindows = 0;
        avgDispersion30 = 0;
        avgImpactBps30 = 0;
        maxDispersion = 0;
        maxImpactBps = 0;
        bufferIndex = 0;
        bufferCount = 0;
        java.util.Arrays.fill(dispersionBuffer, 0);
        java.util.Arrays.fill(impactBpsBuffer, 0);
    }
}
