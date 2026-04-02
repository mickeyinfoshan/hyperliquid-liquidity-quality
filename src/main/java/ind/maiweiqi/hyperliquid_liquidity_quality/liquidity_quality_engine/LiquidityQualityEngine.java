package ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine;

import ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine.datatype.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class LiquidityQualityEngine {

    private final ProductConfig config;
    private final long windowDurationMs;
    private final List<Tick> pendingTicks = new ArrayList<>();
    private final List<WindowProcessedListener> listeners = new CopyOnWriteArrayList<>();
    private final LiquidityQualityStats stats = new LiquidityQualityStats();
    private long lastWindowEnd = -1;
    private Double lastVwap = null;

    public LiquidityQualityEngine(ProductConfig config, long windowDurationMs) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (windowDurationMs <= 0) {
            throw new IllegalArgumentException("windowDurationMs must be positive");
        }
        this.config = config;
        this.windowDurationMs = windowDurationMs;
    }

    public LiquidityQualityEngine(ProductConfig config) {
        this(config, 1000);
    }

    // --- Data Input ---

    public void addTick(Tick tick) {
        pendingTicks.add(tick);
        stats.incrementTotalTicks(1);
    }

    // --- Core Computation ---

    public WindowResult processWindow(long windowEnd) {
        long windowStart = windowEnd - windowDurationMs;

        List<Tick> windowTicks = new ArrayList<>();
        Iterator<Tick> it = pendingTicks.iterator();
        while (it.hasNext()) {
            Tick t = it.next();
            if (t.timestamp() > windowStart && t.timestamp() <= windowEnd) {
                windowTicks.add(t);
            }
        }

        // Prune consumed ticks
        pendingTicks.removeIf(t -> t.timestamp() <= windowEnd);

        if (windowTicks.isEmpty()) {
            lastWindowEnd = windowEnd;
            return null;
        }

        // Unique prices via TreeSet for sorted order
        TreeSet<Double> priceSet = new TreeSet<>();
        double totalVolume = 0;
        double buyVolume = 0;
        double sellVolume = 0;
        double vwapNumerator = 0;

        for (Tick t : windowTicks) {
            priceSet.add(t.price());
            totalVolume += t.volume();
            vwapNumerator += t.price() * t.volume();
            if (t.side() == Side.BUY) {
                buyVolume += t.volume();
            } else {
                sellVolume += t.volume();
            }
        }

        long priceLevels = priceSet.size();
        double highPrice = priceSet.last();
        double lowPrice = priceSet.first();
        double priceRangeAbs = highPrice - lowPrice;
        long priceRangeTicks = Math.round(priceRangeAbs / config.tickSize());
        double vwap = vwapNumerator / totalVolume;
        double delta = buyVolume - sellVolume;
        double deltaRatio = totalVolume > 0 ? delta / totalVolume : 0.0;

        // Impact cost (CME formula) — use previous window's VWAP as reference price
        double impactBps = lastVwap != null
                ? priceLevels * config.tickSize() / lastVwap * 10_000
                : 0;
        double impactDollar = priceLevels * config.tickSize() * config.contractMultiplier();

        QualityGrade grade = QualityGrade.fromImpactBps(impactBps);

        WindowResult result = new WindowResult(
                windowStart, windowEnd,
                priceLevels, priceRangeTicks,
                Math.round(priceRangeAbs * 1_000_000.0) / 1_000_000.0,
                Math.round(impactBps * 10_000.0) / 10_000.0,
                Math.round(impactDollar * 100.0) / 100.0,
                totalVolume, windowTicks.size(), Math.round(vwap * 10_000.0) / 10_000.0,
                highPrice, lowPrice,
                buyVolume, sellVolume, delta,
                Math.round(deltaRatio * 10_000.0) / 10_000.0,
                grade,
                new ArrayList<>(priceSet)
        );

        lastWindowEnd = windowEnd;
        lastVwap = vwap;
        stats.updateFromWindow(result);

        for (WindowProcessedListener listener : listeners) {
            listener.onWindowProcessed(result);
        }

        return result;
    }

    // --- Observer Pattern ---

    public void addListener(WindowProcessedListener listener) {
        listeners.add(listener);
    }

    // --- Accessors ---

    public LiquidityQualityStats getStats() {
        return stats;
    }

    @Override
    public String toString() {
        return "LiquidityQualityEngine('%s', window=%dms, windows=%d, ticks=%d)"
                .formatted(config.symbol(), windowDurationMs, stats.getTotalWindows(), stats.getTotalTicks());
    }
}
