package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WsCandle(
        long t,
        @JsonProperty("T") long closeTime,
        String s,
        String i,
        String o,
        String c,
        String h,
        String l,
        String v,
        int n
) {
}
