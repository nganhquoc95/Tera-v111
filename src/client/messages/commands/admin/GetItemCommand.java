package client.messages.commands.admin;

import java.util.HashMap;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import client.messages.Command;
import constants.GameConstants;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;

public class GetItemCommand extends Command {

    private static final HashMap<String, Integer> potentials = new HashMap<String, Integer>();
    static {
        potentials.put("allstat", 60002);
        potentials.put("ignoredef", 40291);
        potentials.put("dmgboss", 40601);
        potentials.put("peratt", 40051);
    }

    @Override
    public void execute(MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        int itemId = Integer.parseInt(splitted[0]);

        if (!player.isAdmin()) {
            for (int i : GameConstants.itemBlock) {
                if (itemId == i) {
                    player.dropMessage(5, "Sorry but this item is blocked for your GM level.");
                    return;
                }
            }
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.isPet(itemId)) {
            player.dropMessage(5, "Please purchase a pet from the cash shop instead.");
        } else if (!ii.itemExists(itemId)) {
            player.dropMessage(5, itemId + " does not exist");
        } else {
            Item item;
            short flag = (short) ItemFlag.LOCK.getValue();

            if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                if (splitted.length < 1) {
                    player.dropMessage(6, "Syntax: !getitem <potentials>");
                    return;
                }

                Integer potential;
                try {
                    potential = potentials.get(splitted[1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    potential = null;
                }

                Equip equip = ii.randomizeStats_Above((Equip) ii.getEquipById(itemId));
                // item = ii.randomizeStats((Equip) ii.getEquipById(itemId));

                if (potential != null) {
                    equip.setPotential1(potential);
                    equip.setPotential2(potential);
                    equip.setPotential3(potential);
                    equip.setPotential4(potential);
                    equip.setPotential5(potential);
                }

                item = equip;
            } else {
                short quantity;
                try {
                    quantity = (short) Integer.parseInt(splitted[1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    quantity = 1;
                }

                item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
            }

            if (!player.isGM()) {
                item.setFlag(flag);
            }

            if (!player.isAdmin()) {
                item.setOwner(player.getName());
                item.setGMLog(player.getName() + " used !getitem");
            }

            MapleInventoryManipulator.addbyItem(c, item);
        }
        return;
    }
}
