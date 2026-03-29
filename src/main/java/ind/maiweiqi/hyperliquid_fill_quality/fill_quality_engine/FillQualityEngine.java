package ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine;

import ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine.datatype.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FillQualityEngine {

    private final ProductConfig config;
    private final long windowDurationMs;
    private final List<Tick> pendingTicks = new ArrayList<>();
    private final List<WindowResult> results = new ArrayList<>();
    private final List<WindowProcessedListener> listeners = new CopyOnWriteArrayList<>();
    private final FillQualityStats stats = new FillQualityStats();
    private long lastWindowEnd = -1;

    public FillQualityEngine(ProductConfig config, long windowDurationMs) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (windowDurationMs <= 0) {
            throw new IllegalArgumentException("windowDurationMs must be positive");
        }
        this.config = config;
        this.windowDurationMs = windowDurationMs;
    }

    public FillQualityEngine(ProductConfig config) {
        this(config, 1000);
    }

    // --- Data Input ---

    public void addTick(Tick tick) {
        pendingTicks.add(tick);
        stats.incrementTotalTicks(1);
    }

    public void addTicks(List<Tick> ticks) {
        pendingTicks.addAll(ticks);
        stats.incrementTotalTicks(ticks.size());
    }

    public void addOrderEvent(OrderEventType type, long count) {
        stats.recordOrderEvent(type, count);
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
        long totalVolume = 0;
        long buyVolume = 0;
        long sellVolume = 0;
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
        long delta = buyVolume - sellVolume;
        double deltaRatio = totalVolume > 0 ? (double) delta / totalVolume : 0.0;

        // Impact cost (CME formula)
        double impactBps = priceLevels * config.tickSize() / config.refPrice() * 10_000;
        double impactDollar = priceLevels * config.tickSize() * config.contractMultiplier();

        QualityGrade grade = QualityGrade.fromPriceLevels(priceLevels);

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

        results.add(result);
        lastWindowEnd = windowEnd;
        stats.updateFromWindow(result);

        for (WindowProcessedListener listener : listeners) {
            listener.onWindowProcessed(result);
        }

        return result;
    }

    public List<WindowResult> processAllPending() {
        if (pendingTicks.isEmpty()) {
            return List.of();
        }

        long earliest = Long.MAX_VALUE;
        long latest = Long.MIN_VALUE;
        for (Tick t : pendingTicks) {
            if (t.timestamp() < earliest) earliest = t.timestamp();
            if (t.timestamp() > latest) latest = t.timestamp();
        }

        long start = lastWindowEnd >= 0 ? lastWindowEnd : earliest;
        long current = start + windowDurationMs;

        List<WindowResult> processed = new ArrayList<>();
        while (current <= latest + windowDurationMs) {
            WindowResult r = processWindow(current);
            if (r != null) {
                processed.add(r);
            }
            current += windowDurationMs;
        }
        return processed;
    }

    // --- Impact Model ---

    public static double sqrtImpactModel(double orderQtyUsd, double adtv,
                                          double dailyVol, double spreadCost,
                                          double factor) {
        if (adtv <= 0) {
            return spreadCost;
        }
        return spreadCost + factor * dailyVol * Math.sqrt(orderQtyUsd / adtv);
    }

    public static double sqrtImpactModel(double orderQtyUsd, double adtv,
                                          double dailyVol, double spreadCost) {
        return sqrtImpactModel(orderQtyUsd, adtv, dailyVol, spreadCost, 1.0);
    }

    // --- Observer Pattern ---

    public void addListener(WindowProcessedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(WindowProcessedListener listener) {
        listeners.remove(listener);
    }

    // --- Accessors ---

    public FillQualityStats getStats() {
        return stats;
    }

    public List<WindowResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public ProductConfig getConfig() {
        return config;
    }

    public long getWindowDurationMs() {
        return windowDurationMs;
    }

    // --- Reset ---

    public void reset() {
        pendingTicks.clear();
        results.clear();
        stats.reset();
        lastWindowEnd = -1;
    }

    @Override
    public String toString() {
        return "FillQualityEngine('%s', window=%dms, windows=%d, ticks=%d)"
                .formatted(config.symbol(), windowDurationMs, results.size(), stats.getTotalTicks());
    }
}
