package ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine;

import ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine.datatype.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FillQualityEngineTest {

    private ProductConfig config;
    private FillQualityEngine engine;

    @BeforeEach
    void setUp() {
        config = new ProductConfig("BTC", 0.1, 50000.0, 1);
        engine = new FillQualityEngine(config, 1000);
    }

    @Test
    void processWindow_emptyWindow_returnsNull() {
        assertNull(engine.processWindow(10000));
    }

    @Test
    void processWindow_singleTick() {
        engine.addTick(new Tick(50000.0, 5, 5500, Side.BUY));
        WindowResult r = engine.processWindow(6000);

        assertNotNull(r);
        assertEquals(1, r.priceLevels());
        assertEquals(0, r.priceRangeTicks());
        assertEquals(50000.0, r.vwap());
        assertEquals(5, r.totalVolume());
        assertEquals(5, r.buyVolume());
        assertEquals(0, r.sellVolume());
        assertEquals(5, r.delta());
        assertEquals(1, r.tickCount());
        assertEquals(QualityGrade.EXCELLENT, r.qualityGrade());
    }

    @Test
    void processWindow_multipleTicksDifferentPrices() {
        engine.addTick(new Tick(50000.0, 10, 5100, Side.BUY));
        engine.addTick(new Tick(50000.1, 5, 5200, Side.SELL));
        engine.addTick(new Tick(50000.3, 3, 5300, Side.BUY));

        WindowResult r = engine.processWindow(6000);

        assertEquals(3, r.priceLevels());
        assertEquals(3, r.priceRangeTicks()); // 0.3 / 0.1 = 3
        assertEquals(18, r.totalVolume());
        assertEquals(13, r.buyVolume());
        assertEquals(5, r.sellVolume());
        assertEquals(8, r.delta());
        assertEquals(3, r.tickCount());
        assertTrue(r.impactBps() > 0);
        assertEquals(List.of(50000.0, 50000.1, 50000.3), r.uniquePrices());
    }

    @Test
    void processWindow_samePriceTicks_singleLevel() {
        engine.addTick(new Tick(50000.0, 10, 5100, Side.BUY));
        engine.addTick(new Tick(50000.0, 5, 5200, Side.SELL));

        WindowResult r = engine.processWindow(6000);

        assertEquals(1, r.priceLevels());
        assertEquals(0, r.priceRangeTicks());
    }

    @Test
    void processWindow_ticksOutsideWindow_excluded() {
        engine.addTick(new Tick(50000.0, 10, 3000, Side.BUY)); // before window
        engine.addTick(new Tick(50000.1, 5, 5500, Side.BUY));  // in window

        WindowResult r = engine.processWindow(6000);

        assertEquals(1, r.tickCount());
        assertEquals(5, r.totalVolume());
    }

    @Test
    void processWindow_impactCalculation() {
        engine.addTick(new Tick(50000.0, 10, 5100, Side.BUY));
        engine.addTick(new Tick(50000.1, 5, 5200, Side.SELL));

        WindowResult r = engine.processWindow(6000);

        // impact_bps = priceLevels * tickSize / refPrice * 10000
        // = 2 * 0.1 / 50000.0 * 10000 = 0.04 bps
        assertEquals(0.04, r.impactBps(), 0.001);

        // impact_dollar = priceLevels * tickSize * contractMultiplier
        // = 2 * 0.1 * 1 = 0.2
        assertEquals(0.2, r.impactDollar(), 0.001);
    }

    @Test
    void processWindow_vwapCalculation() {
        engine.addTick(new Tick(100.0, 10, 5100, Side.BUY));
        engine.addTick(new Tick(200.0, 10, 5200, Side.BUY));

        ProductConfig cfg = new ProductConfig("TEST", 1.0, 100.0, 1);
        FillQualityEngine eng = new FillQualityEngine(cfg, 1000);
        eng.addTick(new Tick(100.0, 10, 5100, Side.BUY));
        eng.addTick(new Tick(200.0, 10, 5200, Side.BUY));

        WindowResult r = eng.processWindow(6000);
        assertEquals(150.0, r.vwap(), 0.0001);
    }

    @Test
    void processAllPending_generatesCorrectWindows() {
        // Ticks spanning 3 seconds with 1s window
        engine.addTick(new Tick(50000.0, 5, 1100, Side.BUY));
        engine.addTick(new Tick(50000.1, 3, 2100, Side.SELL));
        engine.addTick(new Tick(50000.2, 7, 3100, Side.BUY));

        List<WindowResult> results = engine.processAllPending();

        assertTrue(results.size() >= 2);
        assertEquals(results.size(), engine.getResults().size());
    }

    @Test
    void listenerReceivesWindowResult() {
        List<WindowResult> received = new ArrayList<>();
        engine.addListener(received::add);

        engine.addTick(new Tick(50000.0, 5, 5500, Side.BUY));
        engine.processWindow(6000);

        assertEquals(1, received.size());
        assertEquals(1, received.get(0).priceLevels());
    }

    @Test
    void removeListenerStopsNotifications() {
        List<WindowResult> received = new ArrayList<>();
        WindowProcessedListener listener = received::add;
        engine.addListener(listener);

        engine.addTick(new Tick(50000.0, 5, 5500, Side.BUY));
        engine.processWindow(6000);
        assertEquals(1, received.size());

        engine.removeListener(listener);
        engine.addTick(new Tick(50000.0, 5, 7500, Side.BUY));
        engine.processWindow(8000);
        assertEquals(1, received.size()); // still 1
    }

    @Test
    void rollingStatsAfterMultipleWindows() {
        for (int i = 0; i < 35; i++) {
            long ts = (i + 1) * 1000L + 500;
            engine.addTick(new Tick(50000.0 + i * 0.1, 5, ts, Side.BUY));
            engine.processWindow((i + 2) * 1000L);
        }

        FillQualityStats stats = engine.getStats();
        assertEquals(35, stats.getTotalWindows());
        assertTrue(stats.getAvgDispersion30() > 0);
        assertTrue(stats.getMaxDispersion() >= 1);
    }

    @Test
    void orderEventTracking() {
        engine.addOrderEvent(OrderEventType.SUBMITTED, 100);
        engine.addOrderEvent(OrderEventType.FILLED, 60);
        engine.addOrderEvent(OrderEventType.CANCELLED, 10);
        engine.addOrderEvent(OrderEventType.MODIFIED, 5);

        FillQualityStats stats = engine.getStats();
        assertEquals(100, stats.getOrdersSubmitted());
        assertEquals(60, stats.getOrdersFilled());
        assertEquals(10, stats.getOrdersCancelled());
        assertEquals(5, stats.getOrdersModified());
        assertEquals(60.0, stats.getQfrPct(), 0.01);
        // MEP = (5 * 1 + 10 * 3) / 60 = 35/60 ≈ 0.58
        assertEquals(0.58, stats.getMepRatio(), 0.01);
    }

    @Test
    void sqrtImpactModel_knownValues() {
        double result = FillQualityEngine.sqrtImpactModel(
                1_000_000, 10_000_000, 0.02, 0.001, 1.0);
        // spread + 1.0 * 0.02 * sqrt(1M/10M) = 0.001 + 0.02 * sqrt(0.1)
        // = 0.001 + 0.02 * 0.31623 = 0.001 + 0.006325 = 0.007325
        assertEquals(0.007325, result, 0.0001);
    }

    @Test
    void sqrtImpactModel_zeroAdtv_returnsSpreaCost() {
        assertEquals(0.005, FillQualityEngine.sqrtImpactModel(100, 0, 0.02, 0.005, 1.0));
    }

    @Test
    void reset_clearsAllState() {
        engine.addTick(new Tick(50000.0, 5, 5500, Side.BUY));
        engine.processWindow(6000);
        engine.addOrderEvent(OrderEventType.SUBMITTED, 10);

        engine.reset();

        assertEquals(0, engine.getResults().size());
        assertEquals(0, engine.getStats().getTotalWindows());
        assertEquals(0, engine.getStats().getTotalTicks());
        assertEquals(0, engine.getStats().getOrdersSubmitted());
    }

    @Test
    void qualityGrade_boundaryValues() {
        assertEquals(QualityGrade.EXCELLENT, QualityGrade.fromPriceLevels(1));
        assertEquals(QualityGrade.EXCELLENT, QualityGrade.fromPriceLevels(1.5));
        assertEquals(QualityGrade.GOOD, QualityGrade.fromPriceLevels(2));
        assertEquals(QualityGrade.GOOD, QualityGrade.fromPriceLevels(3));
        assertEquals(QualityGrade.FAIR, QualityGrade.fromPriceLevels(4));
        assertEquals(QualityGrade.FAIR, QualityGrade.fromPriceLevels(5));
        assertEquals(QualityGrade.POOR, QualityGrade.fromPriceLevels(6));
        assertEquals(QualityGrade.POOR, QualityGrade.fromPriceLevels(8));
        assertEquals(QualityGrade.VERY_POOR, QualityGrade.fromPriceLevels(9));
    }

    @Test
    void tick_validationRejectsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> new Tick(100.0, 0, 1000, Side.BUY));
        assertThrows(IllegalArgumentException.class,
                () -> new Tick(100.0, 5, 0, Side.BUY));
        assertThrows(IllegalArgumentException.class,
                () -> new Tick(100.0, 5, 1000, null));
    }

    @Test
    void productConfig_validationRejectsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductConfig("", 0.1, 100.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductConfig("BTC", 0, 100.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductConfig("BTC", 0.1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductConfig("BTC", 0.1, 100.0, 0));
    }

    @Test
    void windowResult_uniquePricesDefensivelyCopied() {
        List<Double> prices = new ArrayList<>(List.of(1.0, 2.0));
        WindowResult r = new WindowResult(0, 1000, 2, 1, 1.0, 0.5, 0.1,
                10, 2, 1.5, 2.0, 1.0, 5, 5, 0, 0.0,
                QualityGrade.EXCELLENT, prices);

        assertThrows(UnsupportedOperationException.class, () -> r.uniquePrices().add(3.0));
    }

    @Test
    void engine_toString() {
        String s = engine.toString();
        assertTrue(s.contains("BTC"));
        assertTrue(s.contains("1000ms"));
    }
}
