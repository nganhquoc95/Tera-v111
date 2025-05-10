/*
	名字:	特殊课程中级生
	地圖:	反抗者本部
	描述:	310010000
*/

function start(mode, type, selection) {
	Packages.server.quest.MapleQuest.getInstance(29942).forceComplete(qm.getPlayer(), qm.getNpc());
	qm.getClient().getSession().write(Packages.tools.packet.CWvsContext.showQuestMsg("<Special Training Intermediate> has been rewarded."));
	qm.getClient().getSession().write(Packages.tools.packet.MaplePacketCreator.serverNotice(5, "<Special Training Intermediate> has been rewarded."));
	qm.dispose();
}

function end(mode, type, selection) {
	qm.dispose();
}