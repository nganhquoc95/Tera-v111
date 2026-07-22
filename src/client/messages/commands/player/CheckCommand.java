package client.messages.commands.player;

import client.MapleCharacter;
import client.MapleClient;
import client.messages.Command;
import scripting.NPCScriptManager;
import tools.FileoutputUtil;
import tools.packet.CWvsContext;
import server.ServerProperties;


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
        String expRate = ServerProperties.getProperty("net.sf.odinms.world.exp");
        String mesoRate = ServerProperties.getProperty("net.sf.odinms.world.meso");
        String dropRate = ServerProperties.getProperty("net.sf.odinms.world.dropRate");

        c.getPlayer().dropMessage(6, "The server rates: EXP: " + expRate + "x, MESO: " + mesoRate + "x, DROP: " + dropRate + "x");
        c.getPlayer().dropMessage(6, "The server time: " + FileoutputUtil.CurrentTimeUTCPlus7() + " (UTC+7)");
        c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getCSPoints(MapleCharacter.CashShopType.NX_CREDIT) + " Cash.");
    }
}