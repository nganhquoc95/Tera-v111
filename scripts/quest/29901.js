/*
	名字:	初階冒險家
	地圖:	維多利亞港
	描述:	104000000
*/

function start(mode, type, selection) {
	Packages.server.quest.MapleQuest.getInstance(29901).forceComplete(qm.getPlayer(), qm.getNpc());
	qm.getClient().getSession().write(Packages.tools.packet.CWvsContext.showQuestMsg("<Junior Adventurer> has been rewarded."));
	qm.getClient().getSession().write(Packages.tools.packet.MaplePacketCreator.serverNotice(5, "You have earned the <Junior Adventurer> title. You can receive a Medal from NPC Dalair."));
	qm.dispose();
}

function end(mode, type, selection) {
	qm.dispose();
}