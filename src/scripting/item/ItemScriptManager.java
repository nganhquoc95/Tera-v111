package scripting.item;

import client.MapleClient;
import server.MapleItemInformationProvider.ScriptedItem;

public class ItemScriptManager {

    private static final ItemScriptManager instance = new ItemScriptManager();

    public static ItemScriptManager getInstance() {
        return instance;
    }

    public void runItemScript(MapleClient c, ScriptedItem scriptItem) {
    }
}
