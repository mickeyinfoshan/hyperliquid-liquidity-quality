package ind.maiweiqi.hyperliquid_liquidity_quality.liquidity_quality_engine;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "liquidity-quality")
public class LiquidityQualityProperties {

    private String symbol = "BTC";
    private double tickSize = 0.1;

    private long contractMultiplier = 1;
    private long windowDurationMs = 1000;
}
