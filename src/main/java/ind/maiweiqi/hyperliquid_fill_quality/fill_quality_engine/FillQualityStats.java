package ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine;

import ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine.datatype.OrderEventType;
import ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine.datatype.WindowResult;
import lombok.Getter;

@Getter
public class FillQualityStats {

    private static final int ROLLING_WINDOW_SIZE = 30;

    // Lifetime counters
    private long totalTicks;
    private long totalVolume;
    private long totalWindows;

    // Order event counters
    private long ordersSubmitted;
    private long ordersFilled;
    private long ordersCancelled;
    private long ordersModified;

    // Rolling statistics
    private double avgDispersion30;
    private double avgImpactBps30;
    private long maxDispersion;
    private double maxImpactBps;

    // QFR & MEP
    private double qfrPct;
    private double mepRatio;

    // Circular buffer for rolling averages
    private final double[] dispersionBuffer = new double[ROLLING_WINDOW_SIZE];
    private final double[] impactBpsBuffer = new double[ROLLING_WINDOW_SIZE];
    private int bufferIndex;
    private int bufferCount;

    public void recordOrderEvent(OrderEventType type, long count) {
        switch (type) {
            case SUBMITTED -> ordersSubmitted += count;
            case FILLED -> ordersFilled += count;
            case CANCELLED -> ordersCancelled += count;
            case MODIFIED -> ordersModified += count;
        }
        recalculateQfrMep();
    }

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

    private void recalculateQfrMep() {
        if (ordersSubmitted > 0) {
            qfrPct = Math.round((double) ordersFilled / ordersSubmitted * 10000.0) / 100.0;
        }
        if (ordersFilled > 0) {
            double msgScore = ordersModified + ordersCancelled * 3.0;
            mepRatio = Math.round(msgScore / ordersFilled * 100.0) / 100.0;
        }
    }

    public void reset() {
        totalTicks = 0;
        totalVolume = 0;
        totalWindows = 0;
        ordersSubmitted = 0;
        ordersFilled = 0;
        ordersCancelled = 0;
        ordersModified = 0;
        avgDispersion30 = 0;
        avgImpactBps30 = 0;
        maxDispersion = 0;
        maxImpactBps = 0;
        qfrPct = 0;
        mepRatio = 0;
        bufferIndex = 0;
        bufferCount = 0;
        java.util.Arrays.fill(dispersionBuffer, 0);
        java.util.Arrays.fill(impactBpsBuffer, 0);
    }
}
