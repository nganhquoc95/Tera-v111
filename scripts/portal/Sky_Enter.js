function enter(pi) {
	var eim = pi.getDisconnected("Dragonica");
	if (eim != null) { //only skip if not null
		eim.registerPlayer(pi.getPlayer());
		return true;
	}
	
	var player = pi.getPlayer();
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
		if (!pi.isLeader()) {
			pi.playerMessage(5, "The leader of the party must be here.");
			return false;
		}
		var party = player.getParty().getMembers();
		var it = party.iterator();
		while (it.hasNext()) {
			var cPlayer = it.next();
			var ccPlayer = player.getMap().getCharacterById(cPlayer.getId());
			
			// Check level
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
		var em = pi.getEventManager("Dragonica");
		if (em == null) {
			pi.playerMessage(5, "This event is currently not available.");
		} else {
			var prop = em.getProperty("state");
			if (prop == null || prop.equals("0")) {
				if (player.getParty() != null) {
					em.startInstance(pi.getParty(), pi.getMap(), 200);
				} else {
					// Solo instance - pass null for party
					em.startInstance(null, pi.getMap(), 200);
				}
			} else {
				pi.playerMessage(5, "Someone is already attempting this boss.");
			}
		}
	} else {
		pi.playerMessage(5, "You must be level 120+ and have Soaring skill.");
		return false;
	}
	return true;
}