/*
 * NPC Name: Tylus / Water Source Statue
 * NPC ID: 2022005
 * Function: Ancient Glacial Water & Ice Ravine Warp (Quest 3122)
 * MapleStory v111
 */

var status = -1;
var questId = 3122; // Hob King Quest ID
var waterItem = 4032649; // Bottle of Ancient Glacial Water
var filledWaterItem = 2022698; // Glacial Water
var targetMap = 921120705; // Ice Ravine Quest Map

function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == 1) {
		status++;
	} else {
		if (status == 0) {
			cm.dispose();
			return;
		}
		status--;
	}

	if (status == 0) {
		// Check if player is on the quest or has completed it
		if (!cm.isQuestActive(questId) && cm.getQuestStatus(questId) != 2) {
			cm.sendOk("The ice here is ancient and freezing cold. You have no reason to touch this water right now.");
			cm.dispose();
			return;
		}

		var text = "The cold air radiates from the ancient ice. What would you like to do?\r\n#b";
		text += "\r\n#L0# Get a Bottle of Ancient Glacial Water.#l";
		text += "\r\n#L1# Go to the Ice Ravine by myself. (Quest Map)#l";
		cm.sendSimple(text);

	} else if (status == 1) {
		if (selection == 0) {
			// Check if player already has the empty bottle or filled water
			if (cm.haveItem(waterItem) || cm.haveItem(filledWaterItem)) {
				cm.sendOk("You already have a bottle of glacial water in your inventory.");
			} else if (!cm.canHold(waterItem)) {
				cm.sendOk("Please make space in your Etc inventory first.");
			} else {
				cm.gainItem(waterItem, 1);
				cm.sendOk("You received an empty #t" + waterItem + "#. Use it in the Ice Ravine to gather the water.");
			}
		} else if (selection == 1) {
			// Warp player to the solo quest map
			cm.playPortalSE();
			cm.warp(targetMap, 0);
		}
		cm.dispose();
	} else {
		cm.dispose();
	}
}