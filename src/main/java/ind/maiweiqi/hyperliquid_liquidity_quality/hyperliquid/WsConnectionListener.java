package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid;

public interface WsConnectionListener {
    void onConnected();

    default void onDisconnected() {}
}
