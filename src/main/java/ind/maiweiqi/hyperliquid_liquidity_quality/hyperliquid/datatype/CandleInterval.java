package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CandleInterval {
    M1("1m"),
    M3("3m"),
    M5("5m"),
    M15("15m"),
    M30("30m"),
    H1("1h"),
    H2("2h"),
    H4("4h"),
    H8("8h"),
    H12("12h"),
    D1("1d"),
    D3("3d"),
    W1("1w"),
    MO1("1M");

    private final String value;

    CandleInterval(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CandleInterval fromValue(String value) {
        for (CandleInterval ci : values()) {
            if (ci.value.equals(value)) {
                return ci;
            }
        }
        throw new IllegalArgumentException("Unknown candle interval: " + value);
    }
}
