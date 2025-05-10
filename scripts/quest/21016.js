/*
	名字:	基礎體力鍛煉2
	地圖:	瑞恩村
	描述:	140000000
*/

var status = -1;

function start(mode, type, selection) {
	switch (mode) {
	case -1:
		qm.dispose();
		return;
	case 0:
		if (status < 1) {
		qm.sendNext("Are you not ready to hunt the #o0100132#s yet? Always proceed if and only if you are fully ready. There's nothing worse than engaging in battles without sufficient preparation.");
		qm.dispose();
		return;
		}
		status--;
		break;
	case 1:
		status++;
		break;
		}
	switch (status) {
	case 0:
		qm.sendAcceptDecline("Shall we continue with your Basic Training? Before accepting, please make sure you have properly equipped your sword and your skills and potions are readily accessible.");
		break;
	case 1:
		qm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(21016)).setStatus(1);
		qm.getPlayer().updateQuest(qm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(21016)), true);
		qm.sendNextS("Alright. This time, let's have you defeat #r#o0100132#s#k, which are slightly more powerful than #o0100131#s. Head over to #b#m140020100##k and defeat #r15#k of them. That should help you build your strength. Alright! Let's do this!", 1);
		break;
	case 2:
		qm.getClient().getSession().write(Packages.tools.packet.CField.EffectPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/guideEffect/aranTutorial/tutorialArrow3"));
		qm.dispose();
}
}