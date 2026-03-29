package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Channel {
    ALL_MIDS("allMids"),
    NOTIFICATION("notification"),
    WEB_DATA3("webData3"),
    TWAP_STATES("twapStates"),
    CLEARINGHOUSE_STATE("clearinghouseState"),
    OPEN_ORDERS("openOrders"),
    CANDLE("candle"),
    L2_BOOK("l2Book"),
    TRADES("trades"),
    ORDER_UPDATES("orderUpdates"),
    USER_EVENTS("userEvents"),
    USER_FILLS("userFills"),
    USER_FUNDINGS("userFundings"),
    USER_NON_FUNDING_LEDGER_UPDATES("userNonFundingLedgerUpdates"),
    ACTIVE_ASSET_CTX("activeAssetCtx"),
    ACTIVE_ASSET_DATA("activeAssetData"),
    USER_TWAP_SLICE_FILLS("userTwapSliceFills"),
    USER_TWAP_HISTORY("userTwapHistory"),
    BBO("bbo"),
    SPOT_STATE("spotState"),
    ALL_DEXS_CLEARINGHOUSE_STATE("allDexsClearinghouseState"),
    ALL_DEXS_ASSET_CTXS("allDexsAssetCtxs"),
    SUBSCRIPTION_RESPONSE("subscriptionResponse");

    private final String value;

    Channel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Channel fromValue(String value) {
        for (Channel c : values()) {
            if (c.value.equals(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown channel: " + value);
    }
}
