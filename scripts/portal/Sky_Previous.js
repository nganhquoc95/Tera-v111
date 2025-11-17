function enter(pi) {
	if (pi.getPlayer().getParty() != null && pi.isLeader()) {
		var currentMapId = pi.getPlayer().getMapId();
		var previousMapId = currentMapId - 100;
		
		// If on the first map (240080400), allow exit to PQ exit map (240080000)
		if (currentMapId == 240080400) {
			pi.warpParty(240080000);
			pi.playPortalSE();
		} else if (previousMapId >= 240080400) {
			// Otherwise, go back to previous stage
			pi.warpParty(previousMapId);
			pi.playPortalSE();
		} else {
			pi.playerMessage(5, "Cannot go back further.");
		}
	} else {
		pi.playerMessage(5, "This portal is not available.");
	}
}