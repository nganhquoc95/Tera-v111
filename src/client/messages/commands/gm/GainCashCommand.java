package client.messages.commands.gm;
import client.messages.Command;
import client.MapleClient;
import client.MapleCharacter;

public class GainCashCommand extends Command { 
    @Override
    public void execute(MapleClient c, String[] splitted){
        MapleCharacter player = c.getPlayer();

        if (splitted.length < 2) {
            c.getPlayer().dropMessage(5, "Need amount.");
            return;
        }

        player.modifyCSPoints(1, Integer.parseInt(splitted[1]), true);
        player.dropMessage(6, "You gained " + splitted[1] + " cash.");
        return;
    }
}