var minPlayers = 3;

function init() {
em.setProperty("state", "0");
	em.setProperty("leader", "true");
}

function setup(level, leaderid) {
	em.setProperty("state", "1");
	em.setProperty("leader", "true");
    var eim = em.newInstance("Dragonica" + leaderid);

	var map1 = eim.setInstanceMap(240080100);
	if (map1 != null) {
		map1.resetPQ(level);
		for (var i = 0; i < 8; i++) {
			var mob1 = em.getMonster(8300000);
			if (mob1 != null) {
				mob1.changeLevel(Math.min(level + 5, 200));
				var spawnPoint = new java.awt.Point(100 + (i * 100), -10);
				map1.spawnMonsterOnGroundBelow(mob1, spawnPoint);
				eim.registerMonster(mob1);
			}
		}
	}

	var map2 = eim.setInstanceMap(240080200);
	if (map2 != null) {
		map2.resetPQ(level);
		for (var i = 0; i < 10; i++) {
			var mob2 = em.getMonster(8300001);
			if (mob2 != null) {
				mob2.changeLevel(Math.min(level + 5, 200));
				var spawnPoint = new java.awt.Point(100 + (i * 100), -10);
				map2.spawnMonsterOnGroundBelow(mob2, spawnPoint);
				eim.registerMonster(mob2);
			}
		}
	}

	var map6 = eim.setInstanceMap(240080600);
	if (map6 != null) {
		map6.resetPQ(level);
	}
	var map7 = eim.setInstanceMap(240080700);
	var map3 = eim.setInstanceMap(240080800);
	if (map3 != null) {
		map3.resetPQ(level);
	    var mob3 = em.getMonster(8300007);
		if (mob3 != null) {
			mob3.changeLevel(level);
			mob3.changeLevel(Math.min(level + 5, 200));
			var bossPoint = new java.awt.Point(700, -10);
			map3.spawnMonsterOnGroundBelow(mob3, bossPoint);
			eim.registerMonster(mob3);
		}
	}

    eim.startEventTimer(1200000); //20 min
    return eim;
}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(0);
    player.changeMap(map, map.getPortal(0));
}

function playerRevive(eim, player) {
    var map = eim.getMapInstance(eim.getMapInstance(0).getAllMonstersThreadsafe().size() == 0 ? 4 : 0);
    player.addHP(50);
    player.changeMap(map, map.getPortal(0));
    return true;
}

function scheduledTimeout(eim) {
    end(eim);
}

function changedMap(eim, player, mapid) {
    if (mapid != 240080600 && mapid != 240080700 && mapid != 240080800 && mapid != 240080040 && mapid != 240080000) {
	eim.unregisterPlayer(player);
	var map = em.getChannelServer().getMapFactory().getMap(240080000);
	player.changeMap(map, map.getPortal(0));

	if (eim.disposeIfPlayerBelow(0, 0)) {
		em.setProperty("state", "0");
		em.setProperty("leader", "true");
	}
    }
}

function playerDisconnected(eim, player) {
    return 0; // Return map index 0 (first map 240080400) to rejoin party members
}

function monsterValue(eim, mobId) {
    return 1;
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);

    if (eim.disposeIfPlayerBelow(0, 0)) {
	em.setProperty("state", "0");
		em.setProperty("leader", "true");
	}
}

function end(eim) {
	em.setProperty("state", "0");
	em.setProperty("leader", "true");
    eim.disposeIfPlayerBelow(100, 240080000);
}

function clearPQ(eim) {
    end(eim);
}

function allMonstersDead(eim) {
	if (eim.getMapInstance(4).getAllMonstersThreadsafe().size() == 0) {
		eim.getMapInstance(4).spawnNpc(2085003, new java.awt.Point(700, -10));
		eim.broadcastPlayerMsg(6, "Dragonica has been beaten! Have the leader go in the Portal to finish!");
	}
}

function leftParty (eim, player) {
    // If only 2 players are left, uncompletable:
	end(eim);
}
function disbandParty (eim) {
	end(eim);
}
function playerDead(eim, player) {}
function cancelSchedule() {}