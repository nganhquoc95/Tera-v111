/*
	名字:	基礎體力鍛煉3
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
		if (status == 5) {
		qm.sendNextS("#b(You declined out of fear, but it's not like you can run away like this. Take a big breath, calm down, and try again.)", 2);
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
		qm.sendNext("It seems like you're warmed up now. This is when rigorous training can really help you build a strong foundation. Let's proceed with the Basic Training, shall we?");
		break;
	case 1:
		qm.sendNextPrev("Go defeat some #r#o0100133#s#k in #b#m140020200##k this time. I think about  #r20#k should do it. Go on ahead and... Hm? Do you have something you'd like to say?");
		break;
	case 2:
		qm.sendNextPrevS("Isn't the number getting bigger and bigger?", 2);
		break;
	case 3:
		qm.sendNextPrev("Of course it is. What, are you not happy with 20? Would you like to defeat 100 of them instead? Oh, how about 999 of them? Someone in Sleepywood would be able to do it easily. After all, we are training...");
		break;
	case 4:
		qm.sendNextPrevS("Oh no, no, no. Twenty is plenty", 2);
		break;
	case 5:
		qm.sendAcceptDecline("You don't have to be so modest. I understand your desire to quickly become the hero you once were. This sort of attitude is what makes you a hero.");
		break;
	case 6:
		Packages.server.quest.MapleQuest.getInstance(21017).forceStart(qm.getPlayer(), qm.getNpc(), null);
		qm.sendNextS("#b(You accepted, thinking you might end up having to 999 of them if you let her keep talking.)#k", 2);
		break;
	case 7:
		qm.sendNextPrevS("Please go ahead and slay 20 #o0100133#s.", 8);
		break;
	case 8:
		qm.getClient().getSession().write(Packages.tools.packet.CField.EffectPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/guideEffect/aranTutorial/tutorialArrow3"));
		qm.dispose();
}
}