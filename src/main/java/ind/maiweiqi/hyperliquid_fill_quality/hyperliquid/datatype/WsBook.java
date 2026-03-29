package ind.maiweiqi.hyperliquid_fill_quality.hyperliquid.datatype;

import java.util.List;

public record WsBook(
        String coin,
        List<List<WsLevel>> levels,
        long time
) {
}
