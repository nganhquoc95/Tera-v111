package client.messages.commands.admin;

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

    @Override
    public void execute(MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        int itemId = Integer.parseInt(splitted[0]);
        short quantity;
        
        try {
            quantity = (short) Integer.parseInt(splitted[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            quantity = 1;
        }

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
                item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
            } else {
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
