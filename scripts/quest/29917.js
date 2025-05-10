/*
	名字:	魔法森林捐獻王
	地圖:	維多利亞港
	描述:	104000000
*/

function start(mode, type, selection) {
	Packages.server.quest.MapleQuest.getInstance(29917).forceComplete(qm.getPlayer(), qm.getNpc());
	qm.getClient().getSession().write(Packages.tools.packet.CWvsContext.showQuestMsg("<Ellinia Donor> has been rewarded."));
	qm.getClient().getSession().write(Packages.tools.packet.MaplePacketCreator.serverNotice(5, "<Ellinia Donor> has been rewarded."));
	qm.dispose();
}

function end(mode, type, selection) {
	qm.dispose();
}