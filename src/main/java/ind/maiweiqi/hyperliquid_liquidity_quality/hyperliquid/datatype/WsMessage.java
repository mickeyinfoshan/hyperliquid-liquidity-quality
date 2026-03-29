package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype;

import com.fasterxml.jackson.databind.JsonNode;

public record WsMessage(
        String channel,
        JsonNode data
) {
}
