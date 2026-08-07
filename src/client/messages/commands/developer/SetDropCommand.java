package client.messages.commands.developer;

import java.sql.Connection;
import java.sql.PreparedStatement;

import client.MapleClient;
import client.messages.Command;
import constants.GameConstants;
import database.DatabaseConnection;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;

public class SetDropCommand extends Command {

    @Override
    public void execute(MapleClient c, String[] splitted) {
        if (splitted.length == 0) {
            c.getPlayer().blueMessage(
                "Syntax: !setdrop <dropperid> <itemid> <questid>(optional)");
            return;
        }

        int itemId;
        int dropperId;
        int questId = 0;

        try {
            dropperId = Integer.parseInt(splitted[0]);
            itemId = Integer.parseInt(splitted[1]);
            if (splitted.length > 2) {
                questId = Integer.parseInt(splitted[2]);
            }
        } catch (NumberFormatException e) {
            c.getPlayer().blueMessage("Your command could not run. Did you only enter numbers?");
            return;
        }

        MapleMonster onemob;
        try {
            onemob = MapleLifeFactory.getMonster(dropperId);
        } catch (RuntimeException e) {
            c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
            return;
        }

        if (onemob == null) {
            c.getPlayer().dropMessage(5, dropperId + " mob does not exist");
            return;
        }

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.isPet(itemId)) {
            c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
            return;
        }

        if (!ii.itemExists(itemId)) {
            c.getPlayer().dropMessage(5, itemId + " does not exist");
            return;
        }

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "INSERT IGNORE INTO drop_data (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES (?, ?, 1, 1, ?, 10000);");
            ps.setInt(1, dropperId);
            ps.setInt(2, itemId);
            ps.setInt(3, questId);
            ps.executeUpdate();
            ps.close();
            con.close();
            c.getPlayer().dropMessage(6, "Successfully added drop for " + onemob.getStats().getName() + " to drop " + itemId + ".");
        } catch (Exception e) {
            c.getPlayer().dropMessage(5, "There was a problem setting the drop. Please try again.");
            e.printStackTrace();
            return;
        }
    }
}