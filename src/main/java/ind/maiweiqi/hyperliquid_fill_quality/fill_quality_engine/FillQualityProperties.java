package ind.maiweiqi.hyperliquid_fill_quality.fill_quality_engine;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "fill-quality")
public class FillQualityProperties {

    private String symbol = "BTC";
    private double tickSize = 0.1;
    private double refPrice = 87000.0;
    private long contractMultiplier = 1;
    private long windowDurationMs = 1000;
}
