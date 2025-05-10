/*
	名字:	正式騎士
	地圖:	耶雷弗
	描述:	130000000
*/

function start(mode, type, selection) {
	Packages.server.quest.MapleQuest.getInstance(29907).forceComplete(qm.getPlayer(), qm.getNpc());
	qm.getClient().getSession().write(Packages.tools.packet.CWvsContext.showQuestMsg("<Official Knight> has been rewarded."));
	qm.getClient().getSession().write(Packages.tools.packet.MaplePacketCreator.serverNotice(5, "<Official Knight> has been rewarded."));
	qm.dispose();
}

function end(mode, type, selection) {
	qm.dispose();
}