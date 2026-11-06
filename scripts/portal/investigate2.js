/*
	名字:	菇菇王國
	地圖:	城壁邊邊
	描述:	106020500
*/

function enter(pi) {
	if (pi.isQuestActive(2322)) {
		var quest = pi.getQuestNoRecord(2322);
		var customData = quest.getCustomData();
		if (customData == null || parseInt(customData, 10) < 1) {
			pi.openNpc(1300014, "1300014");
		}
	}
	return false;
}