/*
    Dragon Rider / Dragon's Nest
*/

function start() {
    cm.sendSimple("#e<Party Quest: Dragon's Nest>#n\r\n\r\n\
        Welcome to the entrance to Crimson Sky. What would you like to do?\r\n\r\n\
        #L0##bEnter Crimson Sky.(Lv 120 or above)#l");
}

function action(mode, type, selection) {
    if (mode > 0) {
        // Allow solo or party entry
        var player = cm.getPlayer();
        var next = true;
        var size = 0;
        
        if (player.getParty() != null) {
            // Party mode - check all members
            if (!cm.isLeader()) {
                cm.sendOk("Only the party leader can start the PQ.");
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
                // Check for Soaring skill - look for any skill 1026 variant based on job
                var hasSkill = false;
                var skillId = 0;
                var jobId = ccPlayer.getJob();
                
                // Determine skill ID based on job
                if (jobId >= 2200 && jobId <= 2209) { // Evan
                    skillId = 20001026;
                } else if (jobId >= 2210 && jobId <= 2212) { // Mercedis
                    skillId = 20021026;
                } else if (jobId >= 3200 && jobId <= 3212) { // Other advanced classes
                    skillId = (Math.floor(jobId / 100) * 10000) + 1026;
                } else {
                    skillId = 1026; // Default
                }
                
                if (ccPlayer.getSkillLevel(skillId) > 0) {
                    hasSkill = true;
                }
                
                if (!hasSkill) {
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
                var skillId = 0;
                var jobId = player.getJob();
                
                // Determine skill ID based on job
                if (jobId >= 2200 && jobId <= 2209) { // Evan
                    skillId = 20001026;
                } else if (jobId >= 2210 && jobId <= 2212) { // Mercedis
                    skillId = 20021026;
                } else if (jobId >= 3200 && jobId <= 3212) { // Other advanced classes
                    skillId = (Math.floor(jobId / 100) * 10000) + 1026;
                } else {
                    skillId = 1026; // Default
                }
                
                if (player.getSkillLevel(skillId) <= 0) {
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
                        em.startInstance(player.getParty(), player.getMap(), 200);
                    } else {
                        // Create a solo instance - pass null for party parameter
                        em.startInstance(null, player.getMap(), 200);
                    }
                } else {
                    cm.sendOk("Someone is already attempting this boss.");
                }
            }
        } else {
            cm.sendOk("You must be level 120+ and have Soaring skill.");
        }
    }
    cm.dispose();
}