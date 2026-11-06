/*
	名字:	彩色菇菇芽孢
	地圖:	菇菇森林深處
	描述:	106020300
*/

var status = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }

    if (mode == 0) {
        cm.dispose();
        return;
    }

    status++;

    if (status == 0) {
        cm.sendYesNo(
            "#bWant to use colored mushroom spores? #k\r\n\r\n" +
            "#r※ #eNotes#n\r\n" +
            "Please do not use it on people! \r\n" +
            "After swallowing by mistake, please go to a nearby hospital for medical treatment as soon as possible!"
        );
    } else if (status == 1) {
        if (!cm.haveItem(2430014, 1)) {
            cm.sendOk("You don't have the required mushroom spores.");
            cm.dispose();
            return;
        }

        var player = cm.getPlayer();
        var portal = player.getMap().getPortal("obstacle");
        if (player.getMapId() != 106020300 || portal == null ||
            portal.getPosition().distance(player.getPosition()) >= 210) {
            cm.sendOk("You must be at the magic barrier to use the mushroom spores.");
            cm.dispose();
            return;
        }

        cm.gainItem(2430014, -1);

        var quest = player.getQuestNAdd(
            Packages.server.quest.MapleQuest.getInstance(2314)
        );
        quest.setCustomData("2");
        player.updateQuest(quest, true);

        cm.getClient().getSession().write(
            Packages.tools.packet.CWvsContext.serverNotice(
                6,
                "The magic barrier has been broken"
            )
        );

        cm.dispose();
    }
}
