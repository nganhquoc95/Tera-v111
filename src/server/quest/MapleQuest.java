package server.quest;

import constants.GameConstants;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import client.MapleCharacter;
import client.MapleQuestStatus;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import scripting.NPCScriptManager;
import tools.Pair;
import tools.Triple;
import tools.packet.CField.EffectPacket;

public class MapleQuest implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private static final Map<Integer, MapleQuest> quests = new LinkedHashMap<Integer, MapleQuest>();
    protected int id;
    protected final List<MapleQuestRequirement> startReqs = new LinkedList<MapleQuestRequirement>();
    protected final List<MapleQuestRequirement> completeReqs = new LinkedList<MapleQuestRequirement>();
    protected final List<MapleQuestAction> startActs = new LinkedList<MapleQuestAction>();
    protected final List<MapleQuestAction> completeActs = new LinkedList<MapleQuestAction>();
    protected final Map<String, List<Pair<String, Pair<String, Integer>>>> partyQuestInfo = new LinkedHashMap<String, List<Pair<String, Pair<String, Integer>>>>(); // [rank,
                                                                                                                                                                    // [more/less/equal,
                                                                                                                                                                    // [property,
                                                                                                                                                                    // value]]]
    protected final Map<Integer, Integer> relevantMobs = new LinkedHashMap<Integer, Integer>();
    private boolean autoStart = false, autoPreComplete = false, repeatable = false, customend = false, blocked = false,
            autoAccept = false, autoComplete = false, scriptedStart = false;
    private int viewMedalItem = 0, selectedSkillID = 0;
    protected String name = "";

    protected MapleQuest(final int id) {
        this.id = id;
    }

    public List<Pair<String, Pair<String, Integer>>> getInfoByRank(final String rank) {
        return partyQuestInfo.get(rank);
    }

    public boolean isPartyQuest() {
        return partyQuestInfo.size() > 0;
    }

    public final int getSkillID() {
        return selectedSkillID;
    }

    public final String getName() {
        return name;
    }

    public final List<MapleQuestAction> getCompleteActs() {
        return completeActs;
    }

    public static void initQuests() {
        long start = System.currentTimeMillis();
        Map<Integer, MapleQuest> newQuests = new LinkedHashMap<Integer, MapleQuest>();

        try (Connection con = DatabaseConnection.getConnection()) {

            // 1. Bulk-load action sub-tables indexed by uniqueid
            Map<Integer, List<MapleQuestAction.QuestItem>> itemsMap = new HashMap<Integer, List<MapleQuestAction.QuestItem>>();
            try (PreparedStatement psi = con.prepareStatement("SELECT * FROM wz_questactitemdata");
                    ResultSet rs = psi.executeQuery()) {
                while (rs.next()) {
                    int uniqueId = rs.getInt("uniqueid");
                    if (!itemsMap.containsKey(uniqueId)) {
                        itemsMap.put(uniqueId, new ArrayList<MapleQuestAction.QuestItem>());
                    }
                    itemsMap.get(uniqueId).add(new MapleQuestAction.QuestItem(
                            rs.getInt("itemid"), rs.getInt("count"), rs.getInt("period"),
                            rs.getInt("gender"), rs.getInt("job"), rs.getInt("jobEx"), rs.getInt("prop")));
                }
            }

            Map<Integer, List<Pair<Integer, Integer>>> questStateMap = new HashMap<Integer, List<Pair<Integer, Integer>>>();
            try (PreparedStatement psq = con.prepareStatement("SELECT * FROM wz_questactquestdata");
                    ResultSet rs = psq.executeQuery()) {
                while (rs.next()) {
                    int uniqueId = rs.getInt("uniqueid");
                    if (!questStateMap.containsKey(uniqueId)) {
                        questStateMap.put(uniqueId, new ArrayList<Pair<Integer, Integer>>());
                    }
                    questStateMap.get(uniqueId).add(new Pair<Integer, Integer>(rs.getInt("quest"), rs.getInt("state")));
                }
            }

            Map<Integer, List<Triple<Integer, Integer, Integer>>> skillMap = new HashMap<Integer, List<Triple<Integer, Integer, Integer>>>();

            try (PreparedStatement pss = con.prepareStatement("SELECT * FROM wz_questactskilldata");
                    ResultSet rs = pss.executeQuery()) {
                while (rs.next()) {
                    int uniqueId = rs.getInt("uniqueid");
                    if (!skillMap.containsKey(uniqueId)) {
                        skillMap.put(uniqueId, new ArrayList<Triple<Integer, Integer, Integer>>());
                    }
                    skillMap.get(uniqueId).add(new Triple<Integer, Integer, Integer>(rs.getInt("skillid"),
                            rs.getInt("skillLevel"), rs.getInt("masterLevel")));
                }
            }

            // 2. Bulk-load wz_questdata into base MapleQuest objects
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM wz_questdata");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int questId = rs.getInt("questid");
                    MapleQuest quest = new MapleQuest(questId);
                    quest.name = rs.getString("name");
                    quest.autoStart = rs.getInt("autoStart") > 0;
                    quest.autoPreComplete = rs.getInt("autoPreComplete") > 0;
                    quest.autoAccept = rs.getInt("autoAccept") > 0;
                    quest.autoComplete = rs.getInt("autoComplete") > 0;
                    quest.viewMedalItem = rs.getInt("viewMedalItem");
                    quest.selectedSkillID = rs.getInt("selectedSkillID");
                    quest.blocked = rs.getInt("blocked") > 0;
                    newQuests.put(questId, quest);
                }
            }

            // 3. Bulk-load wz_questreqdata and attach requirements
            try (PreparedStatement psr = con.prepareStatement("SELECT * FROM wz_questreqdata");
                    ResultSet rs = psr.executeQuery()) {
                while (rs.next()) {
                    int questId = rs.getInt("questid");
                    MapleQuest quest = newQuests.get(questId);
                    if (quest == null) {
                        continue;
                    }
                    MapleQuestRequirementType type = MapleQuestRequirementType.getByWZName(rs.getString("name"));
                    MapleQuestRequirement req = new MapleQuestRequirement(quest, type, rs);

                    if (type == MapleQuestRequirementType.interval) {
                        quest.repeatable = true;
                    } else if (type == MapleQuestRequirementType.normalAutoStart) {
                        quest.repeatable = true;
                        quest.autoStart = true;
                    } else if (type == MapleQuestRequirementType.startscript) {
                        quest.scriptedStart = true;
                    } else if (type == MapleQuestRequirementType.endscript) {
                        quest.customend = true;
                    } else if (type == MapleQuestRequirementType.mob) {
                        for (Pair<Integer, Integer> mob : req.getDataStore()) {
                            quest.relevantMobs.put(mob.left, mob.right);
                        }
                    }

                    if (rs.getInt("type") == 0) {
                        quest.startReqs.add(req);
                    } else {
                        quest.completeReqs.add(req);
                    }
                }
            }

            // 4. Bulk-load wz_questactdata and attach actions using in-memory maps
            try (PreparedStatement psa = con.prepareStatement("SELECT * FROM wz_questactdata");
                    ResultSet rs = psa.executeQuery()) {
                while (rs.next()) {
                    int questId = rs.getInt("questid");
                    MapleQuest quest = newQuests.get(questId);
                    if (quest == null) {
                        continue;
                    }
                    MapleQuestActionType ty = MapleQuestActionType.getByWZName(rs.getString("name"));
                    int actType = rs.getInt("type");

                    if (actType == 0) {
                        if (ty == MapleQuestActionType.item && questId == 7103) {
                            continue;
                        }
                        quest.startActs.add(
                                new MapleQuestAction(ty, rs, quest, itemsMap, questStateMap, skillMap));
                    } else {
                        if (ty == MapleQuestActionType.item && questId == 7102) {
                            continue;
                        }
                        quest.completeActs.add(
                                new MapleQuestAction(ty, rs, quest, itemsMap, questStateMap, skillMap));
                    }
                }
            }

            // 5. Bulk-load wz_questpartydata
            try (PreparedStatement psp = con.prepareStatement("SELECT * FROM wz_questpartydata");
                    ResultSet rs = psp.executeQuery()) {
                while (rs.next()) {
                    int questId = rs.getInt("questid");
                    MapleQuest quest = newQuests.get(questId);
                    if (quest == null) {
                        continue;
                    }
                    String rank = rs.getString("rank");
                    if (!quest.partyQuestInfo.containsKey(rank)) {
                        quest.partyQuestInfo.put(rank, new ArrayList<Pair<String, Pair<String, Integer>>>());
                    }
                    quest.partyQuestInfo.get(rank).add(new Pair<String, Pair<String, Integer>>(
                            rs.getString("mode"),
                            new Pair<String, Integer>(rs.getString("property"), rs.getInt("value"))));
                }
            }

            // Atomic swap
            synchronized (quests) {
                quests.clear();
                quests.putAll(newQuests);
            }

            System.out.println(
                    "Maple Quests loaded (" + quests.size() + ") in " + (System.currentTimeMillis() - start) + "ms.");

        } catch (SQLException e) {
            System.err.println("Error initializing MapleQuests from database:");
            e.printStackTrace();
        }
    }

    public static MapleQuest getInstance(int id) {
        MapleQuest ret = quests.get(id);
        if (ret == null) {
            ret = new MapleQuest(id);
            quests.put(id, ret); // by this time we have already initialized
        }
        return ret;
    }

    public static Collection<MapleQuest> getAllInstances() {
        return quests.values();
    }

    public boolean canStart(MapleCharacter c, Integer npcid) {
        final int[] ss = { 3170, 4576 };// 每日任務ID
        for (int i : ss) {
            if (this.id == i) {
                repeatable = true;
            }
        }
        if (c.getQuest(this).getStatus() != 0 && !(c.getQuest(this).getStatus() == 2 && repeatable)) {
            return false;
        }
        if (blocked && !c.isGM()) {
            return false;
        }
        // if (autoAccept) {
        // return true; //need script
        // }
        for (MapleQuestRequirement r : startReqs) {
            // if (r.getType() == MapleQuestRequirementType.dayByDay && npcid != null) {
            // //everyday. we don't want ok
            // forceComplete(c, npcid);
            // return false;
            // }
            if (!r.check(c, npcid)) {
                return false;
            }
        }
        return true;
    }

    public boolean canComplete(MapleCharacter c, Integer npcid) {
        if (c.getQuest(this).getStatus() != 1) {
            return false;
        }
        if (blocked && !c.isGM() && this.id != 23205) {// 惡魔任務-除掉警備兵！
            return false;
        }
        if (autoComplete && npcid != null && viewMedalItem <= 0) {
            forceComplete(c, npcid);
            return false; // skip script
        }
        for (MapleQuestRequirement r : completeReqs) {
            if (!r.check(c, npcid)) {
                return false;
            }
        }
        return true;
    }

    public final void RestoreLostItem(final MapleCharacter c, final int itemid) {
        if (blocked && !c.isGM()) {
            return;
        }
        for (final MapleQuestAction a : startActs) {
            if (a.RestoreLostItem(c, itemid)) {
                break;
            }
        }
    }

    public void start(MapleCharacter c, int npc) {
        if ((autoStart || checkNPCOnMap(c, npc)) && canStart(c, npc)) {
            for (MapleQuestAction a : startActs) {
                if (!a.checkEnd(c, null)) { // just in case
                    return;
                }
            }
            for (MapleQuestAction a : startActs) {
                a.runStart(c, null);
            }
            if (!customend) {
                forceStart(c, npc, null);
            } else {
                NPCScriptManager.getInstance().endQuest(c.getClient(), npc, getId(), true);
            }
        }
    }

    public void complete(MapleCharacter c, int npc) {
        complete(c, npc, null);
    }

    public void complete(MapleCharacter c, int npc, Integer selection) {// 修復部分非腳本任務不能完成問題
        // if (c.getMap() != null && (autoPreComplete || checkNPCOnMap(c, npc)) &&
        // canComplete(c, npc)) {
        for (MapleQuestAction a : completeActs) {
            if (!a.checkEnd(c, selection)) {
                return;
            }
        }
        forceComplete(c, npc);
        for (MapleQuestAction a : completeActs) {
            a.runEnd(c, selection);
        }
        // we save forfeits only for logging purposes, they shouldn't matter anymore
        // completion time is set by the constructor

        c.getClient().getSession().write(EffectPacket.showForeignEffect(12)); // Quest completion
        c.getMap().broadcastMessage(c, EffectPacket.showForeignEffect(c.getId(), 12), false);
        // }
    }

    public void forfeit(MapleCharacter c) {
        /*
         * if (c.getQuest(this).getStatus() != (byte) 1) {
         * return;
         * }
         */// 外星基地任務暫時注釋
        final MapleQuestStatus oldStatus = c.getQuest(this);
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 0);
        newStatus.setForfeited(oldStatus.getForfeited() + 1);
        newStatus.setCompletionTime(oldStatus.getCompletionTime());
        c.updateQuest(newStatus);
    }

    public void forceStart(MapleCharacter c, int npc, String customData) {
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 1, npc);
        newStatus.setForfeited(c.getQuest(this).getForfeited());
        newStatus.setCompletionTime(c.getQuest(this).getCompletionTime());
        newStatus.setCustomData(customData);
        c.updateQuest(newStatus);
    }

    public void forceComplete(MapleCharacter c, int npc) {
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 2, npc);
        newStatus.setForfeited(c.getQuest(this).getForfeited());
        c.updateQuest(newStatus);
    }

    public int getId() {
        return id;
    }

    public Map<Integer, Integer> getRelevantMobs() {
        return relevantMobs;
    }

    private boolean checkNPCOnMap(MapleCharacter player, int npcid) {
        // mir = 1013000
        return ((GameConstants.isEvan(player.getJob())) && (npcid == 1013000))
                || ((GameConstants.isDemon(player.getJob())) && (npcid == 0))
                || ((GameConstants.isMercedes(player.getJob())) && (npcid == 0)) || (npcid == 2151009)
                || (npcid == 9010000) || ((npcid >= 2161000) && (npcid <= 2161011)) || (npcid == 9000040)
                || (npcid == 9000066) || (npcid == 0)
                || ((player.getMap() != null) && (player.getMap().containsNPC(npcid)));
    }

    public int getMedalItem() {
        return viewMedalItem;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public static enum MedalQuest {

        Beginner(29005, 29015, 15,
                new int[] { 100000000, 100020400, 100040000, 101000000, 101020300, 101040300, 102000000, 102020500,
                        102030400, 102040200, 103000000, 103020200, 103030400, 103040000, 104000000, 104020000,
                        106020100, 120000000, 120020400, 120030000 }),
        ElNath(29006, 29012, 50,
                new int[] { 200000000, 200010100, 200010300, 200080000, 200080100, 211000000, 211030000, 211040300,
                        211041200, 211041800 }),
        LudusLake(29007, 29012, 40,
                new int[] { 222000000, 222010400, 222020000, 220000000, 220020300, 220040200, 221020701, 221000000,
                        221030600, 221040400 }),
        Underwater(29008, 29012, 40,
                new int[] { 230000000, 230010400, 230010200, 230010201, 230020000, 230020201, 230030100, 230040000,
                        230040200, 230040400 }),
        MuLung(29009, 29012, 50,
                new int[] { 251000000, 251010200, 251010402, 251010500, 250010500, 250010504, 250000000, 250010300,
                        250010304, 250020300 }),
        NihalDesert(29010, 29012, 70,
                new int[] { 261030000, 261020401, 261020000, 261010100, 261000000, 260020700, 260020300, 260000000,
                        260010600, 260010300 }),
        MinarForest(29011, 29012, 70,
                new int[] { 240000000, 240010200, 240010800, 240020401, 240020101, 240030000, 240040400, 240040511,
                        240040521, 240050000 }),
        Sleepywood(29014, 29015, 50, new int[] { 105000000, 105000000, 105010100, 105020100, 105020300, 105030000,
                105030100, 105030300, 105030500, 105030500 }); // repeated map

        public int questid, level, lquestid;
        public int[] maps;

        private MedalQuest(int questid, int lquestid, int level, int[] maps) {
            this.questid = questid; // infoquest = questid -2005, customdata = questid -1995
            this.level = level;
            this.lquestid = lquestid;
            this.maps = maps; // note # of maps
        }
    }

    public boolean hasStartScript() {
        return scriptedStart;
    }

    public boolean hasEndScript() {
        return customend;
    }
}
