package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

import java.util.List;

public record WsUserFills(
        boolean isSnapshot,
        String user,
        List<WsFill> fills
) {
}
