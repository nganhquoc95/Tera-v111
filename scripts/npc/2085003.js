/*
    Dragon's Nest PQ - Completion NPC (Dragonica Boss Defeated)
*/

function start() {
    cm.sendSimple("#e<Party Quest: Dragon's Nest - Completion>#n\r\n\r\n\
        Congratulations! You have defeated the boss!\r\n\r\n\
        #L0##bLeave this area#l");
}

function action(mode, type, selection) {
    if (mode > 0) {
        if (cm.getPlayer().getParty() == null || !cm.isLeader()) {
            cm.sendOk("Only the party leader can complete the PQ.");
            cm.dispose();
            return;
        }
        
        // Reward the party
        var chars = cm.getPlayer().getMap().getCharactersThreadsafe();
        for (var i = 0; i < chars.size(); i++) {
            var item = ((chars.get(i).getJob() % 1000) / 100 + 2022651) | 0;
            if (item == 2022651) {
                item = 2022652;
            } else if (item == 2022654) {
                item = 2022655;
            } else if (item == 2022655) {
                item = 2022654;
            }
            cm.gainItem(item, 1, false, 0, 0, "", chars.get(i).getClient());
        }
        cm.addTrait("will", 40);
        cm.addTrait("charisma", 10);
        cm.gainExp_PQ(200, 1.5);
        cm.givePartyNX(1500);
        
        // End the instance and warp out
        var eim = cm.getPlayer().getEventInstance();
        if (eim != null) {
            eim.disposeIfPlayerBelow(100, 240080050);
        }
        
        cm.warpParty(240080050);
    }
    cm.dispose();
}
