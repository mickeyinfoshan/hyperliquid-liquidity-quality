package ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine;

import ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine.datatype.WindowResult;

@FunctionalInterface
public interface WindowProcessedListener {
    void onWindowProcessed(WindowResult result);
}
