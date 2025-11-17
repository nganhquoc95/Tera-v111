function enter(pi) {
	if (pi.getPlayer().getParty() != null && pi.isLeader()) {
		var currentMapId = pi.getPlayer().getMapId();
		var previousMapId = currentMapId - 100;
		
		// Only allow going back if not already in the first map (240080600)
		if (previousMapId >= 240080600) {
			pi.warpParty(previousMapId);
			pi.playPortalSE();
		} else {
			pi.playerMessage(5, "Cannot go back further.");
		}
	} else {
		pi.playerMessage(5, "This portal is not available.");
	}
}