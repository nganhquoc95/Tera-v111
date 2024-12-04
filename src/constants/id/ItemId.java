package constants.id;

import java.util.stream.IntStream;

public class ItemId {

    public static final int NX_CARD_100 = 4031865;
    public static final int NX_CARD_250 = 4031866;

    public static boolean isNxCard(int itemId) {
        return itemId == NX_CARD_100 || itemId == NX_CARD_250;
    }
}
