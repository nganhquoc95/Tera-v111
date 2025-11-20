package client.messages.commands.player;

import client.MapleCharacter;
import client.MapleClient;
import client.messages.Command;
import scripting.NPCScriptManager;
import tools.FileoutputUtil;
import tools.packet.CWvsContext;


public class CheckCommand extends Command{
    {
        setDescription("Show NX and points information, or fix stuck interactive.");
    }

    @Override
    public void execute(MapleClient c, String[] splitted){ 
        c.removeClickedNPC();
        c.getPlayer().setConversation(0);
        NPCScriptManager.getInstance().dispose(c);
        c.getSession().write(CWvsContext.enableActions());

        c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getCSPoints(MapleCharacter.CashShopType.NX_CREDIT) + " Cash.");
        c.getPlayer().dropMessage(6, "The time is currently " + FileoutputUtil.CurrentReadable_TimeGMT() + " GMT.");
    }    
}