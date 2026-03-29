package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WsTrade(
        String coin,
        String side,
        String px,
        String sz,
        String hash,
        long time,
        long tid
) {
}
