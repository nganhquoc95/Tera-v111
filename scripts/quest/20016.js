/*
	名字:	對黑魔法師瞭解嗎？
	地圖:	耶雷弗
	描述:	130000000
*/

var status = -1;

function start(mode, type, selection) {
	switch (mode) {
	case -1:
		qm.dispose();
		return;
	case 0:
		if (status == 8) {
		qm.sendNext("Oh, do you still have some questions? Talk to me again and I'll explain it to you from the very beginning.");
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
		qm.sendNext("Hello, #h0#. Welcome to Cygnus Knights. My name is Neinheart and I am currently serving as the young Empress's Tactician. We'd better get acquainted since we'll be seeing a lot of each other. Haha!");
		break;
	case 1:
		qm.sendNextPrev("I'm sure you have a lot of questions since everything happened so quickly. I'll explain it all, one by one, from where you are to what you're here to do.");
		break;
	case 2:
		qm.sendNextPrev("This island is called Ereve. Thanks to the Empress's magic, this island usually floats around like a boat in the sky and patrols around Maple World. Right now, however, we've stopped here for a reason.");
		break;
	case 3:
		qm.sendNextPrev("The young Empress is the ruler of Maple World. What? This is the first time you've heard of her? Ah, yes. Well, she's the ruler of Maple World but she doesn't like to control it. She watches from afar to make sure that all is well. Well, at least that's her usual role.");
		break;
	case 4:
		qm.sendNextPrev("But that's not the case right now. We've been finding signs all over Maple World that foreshadow the revival of the Black Mage. We can't have the Black Mage come back to terrorize Maple World as he has in the past!");
		break;
	case 5:
		qm.sendNextPrev("But that was ages ago and people today don't realize how scary the Black Mage is. We've all become spoiled by the peaceful Maple World we enjoy today and forgotten how chaotic and frightening Maple World once was. If we don't do something, the Black Mage will once again rule Maple World!");
		break;
	case 6:
		qm.sendNextPrev("This is why the young Empress has decided to take matters into her own hands. She's forming a knighthood of brave Maplers to defeat the Black Mage once and for all. You know what you need to do, right? I'm sure you have an idea since you, yourself, signed up to be a Knight.");
		break;
	case 7:
		qm.sendNextPrev("We have to get stronger so we can defeat the Black Mage if he revives. Our primary goal is to prevent him from destroying Maple World, and you will play a prominent role in that.");
		break;
	case 8:
		qm.sendAcceptDecline("That concludes my explanation. Have I answered all your questions? \r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0# \r\n#fUI/UIWindow.img/QuestIcon/8/0# 380 exp");
		break;
	case 9:
		if (qm.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(20016)).getStatus() < 2) {
			Packages.server.quest.MapleQuest.getInstance(20016).forceComplete(qm.getPlayer(), qm.getNpc());
			qm.getClient().getSession().write(Packages.tools.packet.CWvsContext.getShowQuestCompletion(20016));
			qm.gainExp(380);
			}
			qm.sendNext("I'm glad you're clear about our current situation, but you know, at your current level, you're not even strong enough to face the Black Mage's minions, let alone the Black Mage himself. Not even his minions' minions, as a matter of fact. How will you protect Maple World at your current level?");
			break;
	case 10:
		qm.sendNextPrev("Although you've been accepted into the knighthood, you cannot be recognized as a knight yet. You are not an Official Knight because you're not even a Knight-in-Training. If you remain at your current level, you'll be nothing more than the handyman of Cygnus Knights.");
		break;
	case 11:
		qm.sendNextPrev("But no one starts as a strong Knight on day one. The Empress didn't want someone strong. She wanted someone with courage whom she could develop into a strong Knight through rigorous training. So, you should first become a Knight-in-Training. We'll talk about your missions when you get to that point.");
		break;
	case 12:
		qm.sendPrev("Take the portal on the left to reach the Training Forest. There, you will find Kiku, the Training Instructor, who will teach you how to become stronger. I don't want to find you wandering around aimlessly until you reach Lv. 10, you hear?");
		break;
	case 13:
		qm.dispose();
}
}