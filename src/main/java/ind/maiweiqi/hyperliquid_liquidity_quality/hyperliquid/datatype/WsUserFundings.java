package ind.maiweiqi.hyperliquid_liquidity_quality.hyperliquid.datatype;

import java.util.List;

public record WsUserFundings(
        boolean isSnapshot,
        String user,
        List<WsFunding> fundings
) {
}
