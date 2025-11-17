/*
    Dragon Rider / Dragon's Nest
*/

function start() {
    var player = cm.getPlayer();
    
    // Check if player is already in the PQ
    if (player.getEventInstance() != null) {
        cm.sendYesNo("You are already in the Dragon's Nest PQ. Would you like to quit?");
        return;
    }
    
    var partyInfo = "";
    if (player.getParty() != null) {
        if (cm.isLeader()) {
            partyInfo = "#b(You are the party leader)#n";
        } else {
            partyInfo = "#b(You are a party member - only the leader can start)#n";
        }
    } else {
        partyInfo = "#b(You are not in a party)#n";
    }
    
    cm.sendSimple("#e<Party Quest: Dragon's Nest>#n\r\n\r\n\
        Welcome to the entrance to Crimson Sky. What would you like to do?\r\n\r\n\
        " + partyInfo + "\r\n\r\n\
        #L0##bEnter Crimson Sky.(Lv 120 or above)#l");
}

function action(mode, type, selection) {
    var player = cm.getPlayer();
    
    // Check if player is in the PQ and answering the quit question
    if (player.getEventInstance() != null) {
        if (mode > 0) {
            // User selected Yes to quit
            var eim = player.getEventInstance();
            eim.unregisterPlayer(player);
            var map = cm.getChannelServer().getMapFactory().getMap(240080000);
            player.changeMap(map, map.getPortal(0));
            cm.dispose();
        } else {
            // User selected No, close the NPC
            cm.dispose();
        }
        return;
    }
    
    if (mode > 0) {
        // Allow solo or party entry
        var next = true;
        var size = 0;
        
        // All possible Soaring skill IDs for different job classes
        var soaringSkills = [1026, 10001026, 20001026, 20011026, 20021026, 30001026, 30011026, 50001026];
        
        function hasSoaringSkill(character) {
            for (var i = 0; i < soaringSkills.length; i++) {
                if (character.getSkillLevel(soaringSkills[i]) > 0) {
                    return true;
                }
            }
            return false;
        }
        
        if (player.getParty() != null) {
            // Party mode - check all members
            if (!cm.isLeader()) {
                cm.sendOk("#rOnly the party leader can start this Party Quest.#n\n\nPlease ask your party leader to talk to this NPC.");
                cm.dispose();
                return;
            }
            var party = player.getParty().getMembers();
            var it = party.iterator();
            while (it.hasNext()) {
                var cPlayer = it.next();
                var ccPlayer = player.getMap().getCharacterById(cPlayer.getId());
                // Check if player meets requirements (level 120 and has Soaring skill or higher level skill)
                if (ccPlayer == null || ccPlayer.getLevel() < 120) {
                    next = false;
                    break;
                }
                // Check for Soaring skill
                if (!hasSoaringSkill(ccPlayer)) {
                    next = false;
                    break;
                } else if (ccPlayer.isGM()) {
                    size += 4;
                } else {
                    size++;
                }
            }
        } else {
            // Solo mode - check only the player
            if (player.getLevel() < 120) {
                next = false;
            } else {
                // Check for Soaring skill
                if (!hasSoaringSkill(player)) {
                    next = false;
                } else {
                    size = 1;
                }
            }
        }
        
        if (next && size >= 1) {
            var em = cm.getEventManager("Dragonica");
            if (em == null) {
                cm.sendOk("This event is currently not available.");
            } else {
                var prop = em.getProperty("state");
                if (prop == null || prop.equals("0")) {
                    if (player.getParty() != null) {
                        // var pqMap = cm.getChannelServer().getMapFactory().getMap(240080100);
                        // em.startInstance(player.getParty(), pqMap, 200);
                        em.startInstance(player.getParty(), player.getMap(), 200);
                    } else {
                        // Create a solo instance - pass null for party parameter
                        cm.sendOk("You must be in a party to start this event.");
                    }
                } else {
                    cm.sendOk("Someone is already attempting this boss.");
                }
            }
        } else {
            if (player.getParty() != null) {
                cm.sendOk("#rParty Requirements Not Met#n\n\nAll party members must be:\n- Level 120 or above\n- Have Soaring skill");
            } else {
                cm.sendOk("#rRequirements Not Met#n\n\nYou must be:\n- Level 120 or above\n- Have Soaring skill");
            }
        }
    }
    cm.dispose();
}