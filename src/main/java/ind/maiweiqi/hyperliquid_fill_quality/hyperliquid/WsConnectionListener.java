package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid;

public interface WsConnectionListener {
    void onConnected();

    default void onDisconnected() {}
}
