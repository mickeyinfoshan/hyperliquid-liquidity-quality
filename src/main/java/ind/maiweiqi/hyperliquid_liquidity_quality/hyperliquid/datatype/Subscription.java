package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Subscription(
        Channel type,
        String coin,
        String user,
        String interval,
        Integer nSigFigs,
        Integer mantissa,
        Boolean aggregateByTime,
        String dex,
        Boolean isPortfolioMargin
) {
    // --- Market data subscriptions ---

    public static Subscription allMids() {
        return new Subscription(Channel.ALL_MIDS, null, null, null, null, null, null, null, null);
    }

    public static Subscription trades(String coin) {
        return new Subscription(Channel.TRADES, coin, null, null, null, null, null, null, null);
    }

    public static Subscription l2Book(String coin) {
        return new Subscription(Channel.L2_BOOK, coin, null, null, null, null, null, null, null);
    }

    public static Subscription l2Book(String coin, int nSigFigs, Integer mantissa) {
        return new Subscription(Channel.L2_BOOK, coin, null, null, nSigFigs, mantissa, null, null, null);
    }

    public static Subscription candle(String coin, CandleInterval interval) {
        return new Subscription(Channel.CANDLE, coin, null, interval.getValue(), null, null, null, null, null);
    }

    public static Subscription bbo(String coin) {
        return new Subscription(Channel.BBO, coin, null, null, null, null, null, null, null);
    }

    public static Subscription activeAssetCtx(String coin) {
        return new Subscription(Channel.ACTIVE_ASSET_CTX, coin, null, null, null, null, null, null, null);
    }

    public static Subscription allDexsAssetCtxs() {
        return new Subscription(Channel.ALL_DEXS_ASSET_CTXS, null, null, null, null, null, null, null, null);
    }

    // --- User subscriptions ---

    public static Subscription orderUpdates(String user) {
        return new Subscription(Channel.ORDER_UPDATES, null, user, null, null, null, null, null, null);
    }

    public static Subscription userEvents(String user) {
        return new Subscription(Channel.USER_EVENTS, null, user, null, null, null, null, null, null);
    }

    public static Subscription userFills(String user) {
        return new Subscription(Channel.USER_FILLS, null, user, null, null, null, null, null, null);
    }

    public static Subscription userFills(String user, boolean aggregateByTime) {
        return new Subscription(Channel.USER_FILLS, null, user, null, null, null, aggregateByTime, null, null);
    }

    public static Subscription userFundings(String user) {
        return new Subscription(Channel.USER_FUNDINGS, null, user, null, null, null, null, null, null);
    }

    public static Subscription userNonFundingLedgerUpdates(String user) {
        return new Subscription(Channel.USER_NON_FUNDING_LEDGER_UPDATES, null, user, null, null, null, null, null, null);
    }

    public static Subscription notification(String user) {
        return new Subscription(Channel.NOTIFICATION, null, user, null, null, null, null, null, null);
    }

    public static Subscription webData3(String user) {
        return new Subscription(Channel.WEB_DATA3, null, user, null, null, null, null, null, null);
    }

    public static Subscription openOrders(String user, String dex) {
        return new Subscription(Channel.OPEN_ORDERS, null, user, null, null, null, null, dex, null);
    }

    public static Subscription clearinghouseState(String user, String dex) {
        return new Subscription(Channel.CLEARINGHOUSE_STATE, null, user, null, null, null, null, dex, null);
    }

    public static Subscription twapStates(String user, String dex) {
        return new Subscription(Channel.TWAP_STATES, null, user, null, null, null, null, dex, null);
    }

    public static Subscription activeAssetData(String user, String coin) {
        return new Subscription(Channel.ACTIVE_ASSET_DATA, coin, user, null, null, null, null, null, null);
    }

    public static Subscription userTwapSliceFills(String user) {
        return new Subscription(Channel.USER_TWAP_SLICE_FILLS, null, user, null, null, null, null, null, null);
    }

    public static Subscription userTwapHistory(String user) {
        return new Subscription(Channel.USER_TWAP_HISTORY, null, user, null, null, null, null, null, null);
    }

    public static Subscription spotState(String user) {
        return new Subscription(Channel.SPOT_STATE, null, user, null, null, null, null, null, null);
    }

    public static Subscription spotState(String user, boolean isPortfolioMargin) {
        return new Subscription(Channel.SPOT_STATE, null, user, null, null, null, null, null, isPortfolioMargin);
    }

    public static Subscription allDexsClearinghouseState(String user) {
        return new Subscription(Channel.ALL_DEXS_CLEARINGHOUSE_STATE, null, user, null, null, null, null, null, null);
    }
}
