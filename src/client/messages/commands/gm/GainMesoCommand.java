package client.messages.commands.gm;
import client.messages.Command;
import client.MapleClient;
import client.MapleCharacter;

public class GainMesoCommand extends Command { 
    @Override
    public void execute(MapleClient c, String[] splitted){
        MapleCharacter player = c.getPlayer();
        int amount;

        if (splitted.length < 1){
            player.blueMessage("Syntax: !sp <amount> -- The amount can be negative." );
            return;
        }

        try{
            amount = Math.min(Integer.parseInt(splitted[0]), Integer.MAX_VALUE);
        }
        catch (NumberFormatException e){
            player.dropMessage(6, "That is not a valid number!");
            return;
        }

        if (amount + player.getMeso() > Integer.MAX_VALUE) {
            amount = Integer.MAX_VALUE - player.getMeso();
        }

        player.gainMeso(amount, true);
        player.dropMessage(6, "You gained " + amount + " meso.");
    }
}