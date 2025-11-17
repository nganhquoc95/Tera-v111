function enter(pi) {
	var eim = pi.getDisconnected("Dragonica");
	if (eim != null) { //only skip if not null
		eim.registerPlayer(pi.getPlayer());
		return true;
	}
	
	// Open the NPC 2085001 dialog for the PQ entry
	pi.openNpc(2085001);
	return false;
}