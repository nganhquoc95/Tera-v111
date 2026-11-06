/*
Name:	Mushroom Kingdom
Map:	Deep in the Mushroom Forest
Description:	106020300
*/
function enter(pi) {
	var quest = pi.getPlayer().getQuestNAdd(Packages.server.quest.MapleQuest.getInstance(2314));
	var customData = quest.getCustomData();
	if (quest.getStatus() == 1 && (customData == null || parseInt(customData, 10) < 1)) {
		pi.openNpc(1300014);
	}
	return false;
}