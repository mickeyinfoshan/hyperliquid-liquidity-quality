package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hyperliquid.ws")
public class HyperliquidWebSocketProperties {

    private String url = "wss://api.hyperliquid.xyz/ws";
    private long reconnectDelayMs = 5000;
    private long maxReconnectDelayMs = 60000;
    private long pingIntervalMs = 30000;
}
