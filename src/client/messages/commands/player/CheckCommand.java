package client.messages.commands.player;

import client.MapleCharacter;
import client.MapleClient;
import client.messages.Command;
import server.MapleTrade;
import tools.FileoutputUtil;

public class CheckCommand extends Command{
    {
        setDescription("Show NX and points information, or fix stuck interactive.");
    }

    @Override
    public void execute(MapleClient c, String[] splitted){ 
        // Fix stuck interact by closing trades and player shops
        if (c.getPlayer().getTrade() != null) {
            MapleTrade.cancelTrade(c.getPlayer().getTrade(), c, c.getPlayer());
            c.getPlayer().dropMessage(6, "Trade cancelled.");
        }
        if (c.getPlayer().getPlayerShop() != null) {
            c.getPlayer().getPlayerShop().closeShop(true, true);
            c.getPlayer().dropMessage(6, "Player shop closed.");
        }
        if (c.getPlayer().getShop() != null) {
            c.getPlayer().setShop(null);
            c.getPlayer().dropMessage(6, "Shop closed.");
        }
        if (c.getPlayer().getConversation() > 0) {
            c.getPlayer().setConversation(0);
            c.getPlayer().dropMessage(6, "NPC conversation closed.");
        }
        c.getPlayer().dropMessage(6, "If you were stuck in an interaction, it should be resolved now.");
        c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getCSPoints(MapleCharacter.CashShopType.NX_CREDIT) + " Cash.");
        c.getPlayer().dropMessage(6, "The time is currently " + FileoutputUtil.CurrentReadable_TimeGMT() + " GMT.");
    }    
}