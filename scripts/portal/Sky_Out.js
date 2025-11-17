function enter(pi) {
	var player = pi.getPlayer();
	
	if (player.getParty() != null && pi.isLeader()) {
		pi.warpParty(240080050);
		pi.playPortalSE();
	} else {
		// Non-party leader can quit alone
		pi.warp(240080050, 0);
		pi.playPortalSE();
	}
}
