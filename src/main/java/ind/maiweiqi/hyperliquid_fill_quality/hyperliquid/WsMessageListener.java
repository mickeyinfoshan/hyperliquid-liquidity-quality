package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid;

import ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype.WsMessage;

@FunctionalInterface
public interface WsMessageListener {
    void onMessage(WsMessage message);
}
