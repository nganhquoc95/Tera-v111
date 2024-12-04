package client.messages.commands.player;

import client.MapleClient;
import scripting.NPCScriptManager;
import client.messages.Command;

public class IrvinCommand extends Command {

    // Define the cooldown time in milliseconds (30 minutes).
    private static final long COOLDOWN_TIME = 30 * 60 * 1000;

    {
        setDescription("Warps you to major towns for free at a 30-minute cooldown.");
    }

    @Override
    public void execute(MapleClient c, String[] splitted) {
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().isInBlockedMap()) {
            c.getPlayer().dropMessage(5, "You may not use this command here.");
        } else {
            long currentTime = System.currentTimeMillis();
            long lastUsageTime = c.getPlayer().getIrvinCommandLastUsageTime();

            if (currentTime - lastUsageTime >= COOLDOWN_TIME) {
                // The cooldown has expired, so you can proceed with the command.
                c.getPlayer().setIrvinCommandLastUsageTime(currentTime);
                NPCScriptManager.getInstance().start(c, 9072000);
            } else {
                // The player is still on cooldown. Calculate the remaining time.
                long remainingTime = COOLDOWN_TIME - (currentTime - lastUsageTime);
                int minutes = (int) (remainingTime / (60 * 1000));

                // Notify the player of the remaining cooldown time.
                c.getPlayer().dropMessage(5, "You must wait " + minutes + " minutes before you can use this command again.");
            }
        }
    }
}

