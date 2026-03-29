package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

public record WsRequest(
        String method,
        Subscription subscription
) {
    public static WsRequest subscribe(Subscription subscription) {
        return new WsRequest("subscribe", subscription);
    }

    public static WsRequest unsubscribe(Subscription subscription) {
        return new WsRequest("unsubscribe", subscription);
    }
}
