/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;
import java.awt.Point;

import client.*;
import client.inventory.*;
import client.MapleTrait.MapleTraitType;
import constants.GameConstants;
import client.anticheat.CheatingOffense;
import constants.id.ItemId;
import database.DatabaseConnection;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.awt.Rectangle;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import server.*;
import server.quest.MapleQuest;
import server.maps.SavedLocationType;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.life.MapleMonster;
import server.life.MapleLifeFactory;
import scripting.NPCScriptManager;
import server.shops.HiredMerchant;
import server.shops.IMaplePlayerShop;
import tools.FileoutputUtil;
import tools.Pair;
import tools.packet.MTSCSPacket;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.MobPacket;
import tools.packet.PlayerShopPacket;

public class InventoryHandler {

    private static InventoryHandlerAction action = null;

    public static final void ItemMove(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { // hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte()); // 04
        final short src = slea.readShort(); // 01 00
        final short dst = slea.readShort(); // 00 00
        final short quantity = slea.readShort(); // 53 01

        if (src < 0 && dst > 0) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, src, dst);
        }
    }

    public static final void SwitchBag(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { // hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        final short src = (short) slea.readInt(); // 01 00
        final short dst = (short) slea.readInt(); // 00 00
        if (src < 100 || dst < 100) {
            return;
        }
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, src, dst);
    }

    public static final void MoveBag(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { // hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        final boolean srcFirst = slea.readInt() > 0;
        short dst = (short) slea.readInt(); // 01 00
        if (slea.readByte() != 4) { // must be etc) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        short src = slea.readShort(); // 00 00
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, srcFirst ? dst : src, srcFirst ? src : dst);
    }

    public static final void ItemSort(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final MapleInventoryType pInvType = MapleInventoryType.getByType(slea.readByte());
        if (pInvType == MapleInventoryType.UNDEFINED || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final MapleInventory pInv = c.getPlayer().getInventory(pInvType); // Mode should correspond with
        // MapleInventoryType
        boolean sorted = false;

        while (!sorted) {
            final byte freeSlot = (byte) pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                byte itemSlot = -1;
                for (byte i = (byte) (freeSlot + 1); i <= pInv.getSlotLimit(); i++) {
                    if (pInv.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, itemSlot, freeSlot);
                } else {
                    sorted = true;
                }
            } else {
                sorted = true;
            }
        }
        c.getSession().write(CWvsContext.finishedSort(pInvType.getType()));
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void ItemGather(final LittleEndianAccessor slea, final MapleClient c) {
        // [41 00] [E5 1D 55 00] [01]
        // [32 00] [01] [01] // Sent after

        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        if (c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final byte mode = slea.readByte();
        final MapleInventoryType invType = MapleInventoryType.getByType(mode);
        MapleInventory Inv = c.getPlayer().getInventory(invType);

        final List<Item> itemMap = new LinkedList<>();
        Inv.list().forEach(item -> {
            itemMap.add(item.copy()); // clone all items T___T.
        });
        itemMap.forEach(itemStats -> {
            MapleInventoryManipulator.removeFromSlot(c, invType, itemStats.getPosition(), itemStats.getQuantity(), true,
                    false);
        });

        final List<Item> sortedItems = sortItems(itemMap);
        sortedItems.forEach(item -> {
            MapleInventoryManipulator.addFromDrop(c, item, false);
        });
        c.getSession().write(CWvsContext.finishedGather(mode));
        c.getSession().write(CWvsContext.enableActions());
        itemMap.clear();
        sortedItems.clear();
    }

    private static List<Item> sortItems(final List<Item> passedMap) {
        final List<Integer> itemIds = new ArrayList<>(); // empty list.
        passedMap.forEach(item -> {
            itemIds.add(item.getItemId()); // adds all item ids to the empty list to be sorted.
        });
        Collections.sort(itemIds); // sorts item ids

        final List<Item> sortedList = new LinkedList<>(); // ordered list pl0x <3.

        for (Integer val : itemIds) {
            for (Item item : passedMap) {
                if (val == item.getItemId()) { // Goes through every index and finds the first value that matches
                    sortedList.add(item);
                    passedMap.remove(item);
                    break;
                }
            }
        }
        return sortedList;
    }

    public static final void UseItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMapId() == 749040100 || chr.getMap() == null
                || chr.hasDisease(MapleDisease.POTION) || chr.hasBlockedInventory() || chr.inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
            chr.dropMessage(5, "You may not use this item yet.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { // cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }

        } else {
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    public static final boolean UseRewardItem(final byte slot, final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        c.getSession().write(CWvsContext.enableActions());
        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && !chr.hasBlockedInventory()) {
            if (chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.USE).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.SETUP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.ETC).getNextFreeSlot() > -1) {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final Pair<Integer, List<StructRewardItem>> rewards = ii.getRewardItem(itemId);

                if (rewards != null && rewards.getLeft() > 0) {
                    while (true) {
                        for (StructRewardItem reward : rewards.getRight()) {
                            if (reward.prob > 0 && Randomizer.nextInt(rewards.getLeft()) < reward.prob) { // Total prob
                                if (itemId == 2550000) { //슈피겔만의 뱃지 상자
                                    Equip e = (Equip) MapleItemInformationProvider.getInstance().getEquipById(reward.itemid);
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setAcc((short) Randomizer.rand(1, 6));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setAvoid((short) Randomizer.rand(1, 6));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setDex((short) Randomizer.rand(1, 6));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setHands((short) Randomizer.rand(1, 6));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setHp((short) (Randomizer.rand(1, 10) * 10));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setInt((short) Randomizer.rand(1, 6));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setMp((short) (Randomizer.rand(1, 10) * 10));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setJump((short) Randomizer.rand(1, 10));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setLuk((short) Randomizer.rand(1, 6));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setMatk((short) Randomizer.rand(1, 10));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setMdef((short) Randomizer.rand(1, 10));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setSpeed((short) Randomizer.rand(5, 20));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setStr((short) Randomizer.rand(1, 6));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setWatk((short) Randomizer.rand(1, 10));
                                    }
                                    if (Randomizer.rand(0, 2) == 1) {
                                        e.setWdef((short) Randomizer.rand(1, 10));
                                    }
                                    e.setExpiration(7 * 86400 * 1000 + System.currentTimeMillis());
                                    MapleInventoryManipulator.addFromDrop(c, e, false);
                                    c.getPlayer().GmText(7, GameConstants.getENGType(reward.itemid) + " Get items (" + MapleItemInformationProvider.getInstance().getName(reward.itemid) + ")");
                                } else if (GameConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP) {
                                    final Item item = ii.getEquipById(reward.itemid);
                                    if (reward.period > 0) {
                                        item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
                                    }
                                    item.setGMLog("Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                    c.getPlayer().GmText(7, GameConstants.getENGType(reward.itemid) + " Get items (" + MapleItemInformationProvider.getInstance().getName(reward.itemid) + ")");
                                    MapleInventoryManipulator.addbyItem(c, item);
                                } else {
                                    c.getPlayer().GmText(7, GameConstants.getENGType(reward.itemid) + " Get items (" + MapleItemInformationProvider.getInstance().getName(reward.itemid) + ")");
                                    MapleInventoryManipulator.addById(c, reward.itemid, reward.quantity, "Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                }
                                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemId), itemId, 1, false, false);

                                c.getSession().write(CField.EffectPacket.showRewardItemAnimation(reward.itemid, reward.effect));
                                chr.getMap().broadcastMessage(chr, CField.EffectPacket.showRewardItemAnimation(reward.itemid, reward.effect, chr.getId()), false);
                                return true;
                            }
                        }
                    }
                } else {
                    chr.dropMessage(6, "Unknown error.");
                }
            } else {
                chr.dropMessage(5, "請預留一定的背包空間。");
            }
        }
        return false;
    }

    public static final void UseCosmetic(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory() || chr.inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 254
                || (itemId / 1000) % 10 != chr.getGender()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
    }

    public static final void UseReturnScroll(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        if (!chr.isAlive() || chr.getMapId() == 749040100 || chr.hasBlockedInventory() || chr.isInBlockedMap()
                || chr.inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }

        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyReturnScroll(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            } else {
                c.getSession().write(CWvsContext.enableActions());
            }
        } else {
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    public static final void UseAPResetScroll(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter chr = c.getPlayer();
        c.getPlayer().updateTick(slea.readInt());
        final short slot = slea.readShort();
        if (slot < 1 && slot > 96) {//hmm
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final Item item = chr.getInventory(MapleInventoryType.USE).getItem((byte) slot);
        final int itemId = slea.readInt();
        if (itemId / 1000 != 2501 || item == null || item.getItemId() != itemId) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (byte) slot, (short) 1, true);
        //final int jobId = chr.getJob().getId();
        int newAp = chr.getStat().getStr() - 4;
        newAp += chr.getStat().getDex() - 4;
        newAp += chr.getStat().getInt() - 4;
        newAp += chr.getStat().getLuk() - 4;
        chr.getStat().setStr((short) 4, chr);
        chr.getStat().setDex((short) 4, chr);
        chr.getStat().setInt((short) 4, chr);
        chr.getStat().setLuk((short) 4, chr);
        chr.setRemainingAp((short) newAp);
        Map<MapleStat, Integer> stat = new EnumMap<MapleStat, Integer>(MapleStat.class);
        stat.put(MapleStat.STR, 4);
        stat.put(MapleStat.DEX, 4);
        stat.put(MapleStat.INT, 4);
        stat.put(MapleStat.LUK, 4);
        stat.put(MapleStat.AVAILABLEAP, newAp);
        c.getSession().write(CWvsContext.updatePlayerStats(stat, false, chr));

    }

    public static final void UseSPResetScroll(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter chr = c.getPlayer();
        c.getPlayer().updateTick(slea.readInt());
        final short slot = slea.readShort();
        if (slot < 1 && slot > 96) {//hmm
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final Item item = chr.getInventory(MapleInventoryType.USE).getItem((byte) slot);
        final int itemId = slea.readInt();
        if (item.getItemId() / 1000 != 2500 || item == null || item.getItemId() != itemId || GameConstants.isBeginnerJob(chr.getJob())) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int[] spToGive = chr.getRemainingSps();
        int skillshit = 0;
        int skillLevel;
        final List<Skill> toRemove = new ArrayList<Skill>();
        for (Skill skill : chr.getSkills().keySet()) {
            if (!skill.isBeginnerSkill() && skill.getId() / 10000000 != 9) {
                skillLevel = chr.getSkillLevel(skill);
                if (skillLevel > 0) {
                    skillshit = skillLevel;
                }
                spToGive[GameConstants.getSkillBookForSkill(skill.getId())] += skillLevel;
                toRemove.add(skill);
            }
        }
        for (Skill skill : toRemove) {
            chr.changeSingleSkillLevel(skill, -1, (byte) -1, -1);
        }
        if (skillshit == 0 && spToGive[0] == 0 && chr.getLevel() > 10) {
            if (GameConstants.isExtendedSPJob(chr.getJob())) {
                chr.dropMessage(1, "This class cannot reset SP.");
            } else {
                int sp = 1;
                sp += (chr.getLevel() - (chr.getJob() / 100 % 10 == 2 ? 8 : 10)) * 3;
                if (sp < 0) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                sp += (chr.getJob() % 100 != 0 && chr.getJob() % 100 != 1) ? ((chr.getJob() % 10) + 1) : 0;
                if (chr.getJob() % 10 >= 2) {
                    sp += 2;
                }
                spToGive[0] = sp;
            }
        }
        chr.baseSkills();
        for (int i = 0; i < spToGive.length; i++) {
            chr.setRemainingSp(spToGive[i], i);
        }
        chr.updateSingleStat(MapleStat.AVAILABLESP, 0);//lol
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (byte) slot, (short) 1, true);
    }

    public static final void UseAlienSocket(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final Item alienSocket = c.getPlayer().getInventory(MapleInventoryType.USE).getItem((byte) slea.readShort());
        final int alienSocketId = slea.readInt();
        final Item toMount = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readShort());
        if (alienSocket == null || alienSocketId != alienSocket.getItemId() || toMount == null
                || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        // Can only use once-> 2nd and 3rd must use NPC.
        final Equip eqq = (Equip) toMount;
        if (eqq.getSocketState() != 0) { // Used before
            c.getPlayer().dropMessage(1, "This item already has a socket.");
        } else {
            eqq.setSocket1(0); // First socket, GMS removed the other 2
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, alienSocket.getPosition(), (short) 1,
                    false);
            c.getPlayer().forceReAddItem(toMount, MapleInventoryType.EQUIP);
        }
        c.getSession().write(MTSCSPacket.useAlienSocket(true));
    }

    public static final void UseNebulite(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final Item nebulite = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((byte) slea.readShort());
        final int nebuliteId = slea.readInt();
        final Item toMount = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readShort());
        if (nebulite == null || nebuliteId != nebulite.getItemId() || toMount == null
                || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        final Equip eqq = (Equip) toMount;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        boolean success = false;
        if (eqq.getSocket1() == 0/* || eqq.getSocket2() == 0 || eqq.getSocket3() == 0 */) { // GMS removed 2nd and 3rd
            // sockets, we can put into
            // npc.
            final StructItemOption pot = ii.getSocketInfo(nebuliteId);
            if (pot != null && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId())) {
                // if (eqq.getSocket1() == 0) { // priority comes first
                eqq.setSocket1(pot.opID);
                // }// else if (eqq.getSocket2() == 0) {
                // eqq.setSocket2(pot.opID);
                // } else if (eqq.getSocket3() == 0) {
                // eqq.setSocket3(pot.opID);
                // }
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite.getPosition(), (short) 1,
                        false);
                c.getPlayer().forceReAddItem(toMount, MapleInventoryType.EQUIP);
                success = true;
            }
        }
        c.getPlayer().getMap().broadcastMessage(CField.showNebuliteEffect(c.getPlayer().getId(), success));
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void UseNebuliteFusion(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final int nebuliteId1 = slea.readInt();
        final Item nebulite1 = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((byte) slea.readShort());
        final int nebuliteId2 = slea.readInt();
        final Item nebulite2 = c.getPlayer().getInventory(MapleInventoryType.SETUP).getItem((byte) slea.readShort());
        final int mesos = slea.readInt();
        final int premiumQuantity = slea.readInt();
        if (nebulite1 == null || nebulite2 == null || nebuliteId1 != nebulite1.getItemId()
                || nebuliteId2 != nebulite2.getItemId() || (mesos == 0 && premiumQuantity == 0)
                || (mesos != 0 && premiumQuantity != 0) || mesos < 0 || premiumQuantity < 0
                || c.getPlayer().hasBlockedInventory()) {
            c.getPlayer().dropMessage(1, "Failed to fuse Nebulite.");
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        final int grade1 = GameConstants.getNebuliteGrade(nebuliteId1);
        final int grade2 = GameConstants.getNebuliteGrade(nebuliteId2);
        final int highestRank = grade1 > grade2 ? grade1 : grade2;
        if (grade1 == -1 || grade2 == -1 || (highestRank == 3 && premiumQuantity != 2)
                || (highestRank == 2 && premiumQuantity != 1) || (highestRank == 1 && mesos != 5000)
                || (highestRank == 0 && mesos != 3000) || (mesos > 0 && c.getPlayer().getMeso() < mesos)
                || (premiumQuantity > 0 && c.getPlayer().getItemQuantity(4420000, false) < premiumQuantity)
                || grade1 >= 4 || grade2 >= 4
                || (c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 1)) { // 4000 + = S, 3000 +
            // = A, 2000 + = B,
            // 1000 + = C, else =
            // D
            c.getSession().write(CField.useNebuliteFusion(c.getPlayer().getId(), 0, false));
            return; // Most of them were done in client, so we just send the unsuccessfull packet,
            // as it is only here when they packet edit.
        }
        final int avg = (grade1 + grade2) / 2; // have to revise more about grades.
        final int rank = Randomizer.nextInt(100) < 4
                ? (Randomizer.nextInt(100) < 70 ? (avg != 3 ? (avg + 1) : avg) : (avg != 0 ? (avg - 1) : 0))
                : avg;
        // 4 % chance to up/down 1 grade, (70% to up, 30% to down), cannot up to S
        // grade. =)
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final List<StructItemOption> pots = new LinkedList<>(ii.getAllSocketInfo(rank).values());
        int newId = 0;
        while (newId == 0) {
            StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
            if (pot != null) {
                newId = pot.opID;
            }
        }
        if (mesos > 0) {
            c.getPlayer().gainMeso(-mesos, true);
        } else if (premiumQuantity > 0) {
            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4420000, premiumQuantity, false, false);
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite1.getPosition(), (short) 1,
                false);
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, nebulite2.getPosition(), (short) 1,
                false);
        MapleInventoryManipulator.addById(c, newId, (short) 1,
                "Fused from " + nebuliteId1 + " and " + nebuliteId2 + " on " + FileoutputUtil.CurrentReadable_Date());
        c.getSession().write(CField.useNebuliteFusion(c.getPlayer().getId(), newId, true));
    }

    public static final void UseMagnify(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final byte src = (byte) slea.readShort();
        final boolean insight = src == 127 && c.getPlayer().getTrait(MapleTraitType.sense).getLevel() >= 30;
        final Item magnify = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(src);
        final Item toReveal = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readShort());
        if ((magnify == null && !insight) || toReveal == null || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            return;
        }
        final Equip eqq = (Equip) toReveal;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final int reqLevel = ii.getReqLevel(eqq.getItemId()) / 10;
        if (eqq.getState() == 1
                && (insight || magnify.getItemId() == 2460003 || (magnify.getItemId() == 2460002 && reqLevel <= 12)
                || (magnify.getItemId() == 2460001 && reqLevel <= 7)
                || (magnify.getItemId() == 2460000 && reqLevel <= 3))) {
            final List<List<StructItemOption>> pots = new LinkedList<>(ii.getAllPotentialInfo().values());
            int new_state = Math.abs(eqq.getPotential1());
            if (new_state > 20 || new_state < 17) { // incase overflow
                new_state = 17;
            }
            int lines = 2; // default
            if (eqq.getPotential2() != 0) {
                lines++;
            }
            if (eqq.getPotential3() != 0) {
                lines++;
            }
            if (eqq.getPotential4() != 0) {
                lines++;
            }
            while (eqq.getState() != new_state) {
                // 31001 = haste, 31002 = door, 31003 = se, 31004 = hb, 41005 = combat orders,
                // 41006 = advanced blessing, 41007 = speed infusion
                for (int i = 0; i < lines; i++) { // minimum 2 lines, max 5
                    boolean rewarded = false;
                    while (!rewarded) {
                        StructItemOption pot = pots.get(Randomizer.nextInt(pots.size())).get(reqLevel);
                        if (pot != null && pot.reqLevel / 10 <= reqLevel
                                && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId())
                                && GameConstants.potentialIDFits(pot.opID, new_state, i)) { // optionType
                            // have to research optionType before making this truely official-like
                            switch (i) {
                                case 0 ->
                                    eqq.setPotential1(pot.opID);
                                case 1 ->
                                    eqq.setPotential2(pot.opID);
                                case 2 ->
                                    eqq.setPotential3(pot.opID);
                                case 3 ->
                                    eqq.setPotential4(pot.opID);
                                case 4 ->
                                    eqq.setPotential5(pot.opID);
                                default -> {
                                }
                            }
                            rewarded = true;
                        }
                    }
                }
            }
            c.getPlayer().getTrait(MapleTraitType.insight)
                    .addExp((insight ? 10 : ((magnify.getItemId() + 2) - 2460000)) * 2, c.getPlayer());
            c.getPlayer().getMap()
                    .broadcastMessage(CField.showMagnifyingEffect(c.getPlayer().getId(), eqq.getPosition()));
            if (!insight) {
                c.getSession().write(InventoryPacket.scrolledItem(magnify, toReveal, false, true));
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, magnify.getPosition(), (short) 1,
                        false);
            } else {
                c.getPlayer().forceReAddItem(toReveal, MapleInventoryType.EQUIP);
            }
            c.getSession().write(CWvsContext.enableActions());
        } else {
            c.getSession().write(InventoryPacket.getInventoryFull());
        }
    }

    public static final void addToScrollLog(int accountID, int charID, int scrollID, int itemID, byte oldSlots,
            byte newSlots, byte viciousHammer, String result, boolean ws, boolean ls, int vega) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection()
                    .prepareStatement("INSERT INTO scroll_log VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, accountID);
                ps.setInt(2, charID);
                ps.setInt(3, scrollID);
                ps.setInt(4, itemID);
                ps.setByte(5, oldSlots);
                ps.setByte(6, newSlots);
                ps.setByte(7, viciousHammer);
                ps.setString(8, result);
                ps.setByte(9, (byte) (ws ? 1 : 0));
                ps.setByte(10, (byte) (ls ? 1 : 0));
                ps.setInt(11, vega);
                ps.execute();
            }
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
        }
    }

    public static void UseProtectShield(LittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        slea.skip(1);
        boolean use = false;
        boolean legendarySpirit = false; //장인의혼 사용여부
        Equip toScroll;
        Equip.ScrollResult scrollSuccess = Equip.ScrollResult.SUCCESS; //무조건 성공
        if (dst < 0) {
            toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else {
            legendarySpirit = true;
            toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        Item scroll = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (scroll == null || !GameConstants.isSpecialCSScroll(scroll.getItemId())) {
            scroll = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
            use = true;
        }
        if (!use) {
            if (scroll.getItemId() == 5064000 || scroll.getItemId() == 2531000) {
                toScroll.addAttribute(ItemFlag.PROTECT.getValue());
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5064100) {
                toScroll.addAttribute(ItemFlag.SAFETY.getValue());
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5064300) {
                toScroll.addAttribute(ItemFlag.RECOVERY.getValue());
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5063000) {
                toScroll.addAttribute(ItemFlag.LUKCYDAY.getValue());
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5063100) {
                short flag = toScroll.getFlag();
                if (!ItemFlag.LUKCYDAY.check(flag) && !ItemFlag.PROTECT.check(flag)) {
                    flag |= ItemFlag.LUKCYDAY.getValue();
                    flag |= ItemFlag.PROTECT.getValue();
                    toScroll.setFlag(flag);
                    c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
                } else {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
            }
            c.getPlayer().getInventory(MapleInventoryType.CASH).removeItem(scroll.getPosition(), (short) 1, false);
        } else {
            if (scroll.getItemId() == 2531000) {
                toScroll.addAttribute(ItemFlag.PROTECT.getValue());
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 5064200 || scroll.getItemId() == 2049600 || scroll.getItemId() == 2049601 || scroll.getItemId() == 2049604) {
                Equip origin = (Equip) MapleItemInformationProvider.getInstance().getEquipById(toScroll.getItemId());
                origin.setDurability(toScroll.getDurability());
                origin.setExpiration(toScroll.getExpiration());
                origin.setFlag(toScroll.getFlag());
                origin.setPotential1(toScroll.getPotential1());
                origin.setPotential2(toScroll.getPotential2());
                origin.setPotential3(toScroll.getPotential3());
                origin.setPotential4(toScroll.getPotential4());
                origin.setPotential5(toScroll.getPotential5());
                toScroll = origin;
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else  if (scroll.getItemId() == 2532000) {
                toScroll.addAttribute(ItemFlag.SAFETY.getValue());
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            } else if (scroll.getItemId() == 2530000 || scroll.getItemId() == 2530001 || scroll.getItemId() == 2530002) {
                toScroll.addAttribute(ItemFlag.LUKCYDAY.getValue());
                c.getSession().write(InventoryPacket.updateSpecialItemUse(toScroll, toScroll.getType(), c.getPlayer()));
            }
            c.getPlayer().getInventory(MapleInventoryType.USE).removeItem(scroll.getPosition(), (short) 1, false);
        }
        c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, false, false));
        c.getPlayer().getMap().broadcastMessage(CField.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit, false));
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final boolean UseUpgradeScroll(final short slot, final short dst, final short ws, final MapleClient c, final MapleCharacter chr, final boolean legendarySpirit, final boolean cash) {
        return UseUpgradeScroll(slot, dst, ws, c, chr, 0, legendarySpirit, cash);
    }

    public static final boolean UseUpgradeScroll(final short slot, final short dst, final short ws, final MapleClient c, final MapleCharacter chr, final int vegas, final boolean legendarySpirit, final boolean cash) {
        boolean whiteScroll = false; // white scroll being used?
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        chr.setScrolledPosition((short) 0);
        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        Equip toScroll = null;
        if (dst < 0) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else if (legendarySpirit) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        if (toScroll == null || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("Test 1");
            return false;
        }
        final byte oldLevel = toScroll.getLevel();
        final byte oldEnhance = toScroll.getEnhance();
        final byte oldState = toScroll.getState();
        final short oldFlag = toScroll.getFlag();
        final byte oldSlots = toScroll.getUpgradeSlots();
        boolean SAFETY = false;
        boolean RECOVERY = false;
        Item scroll = cash ? chr.getInventory(MapleInventoryType.CASH).getItem(slot) : chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (scroll == null) {
            scroll = cash ? chr.getInventory(MapleInventoryType.USE).getItem(slot) : chr.getInventory(MapleInventoryType.CASH).getItem(slot);
            if (scroll == null) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("Test 2");
                return false;
            }
        }
        if (scroll.getItemId() == 5064200 || scroll.getItemId() == 2049600 || scroll.getItemId() == 2049601 || scroll.getItemId() == 2049604) {
            int success = ii.getScrollSuccess(scroll.getItemId());
            if (scroll.getItemId() == 5064200) {
                success = 100;
            }
            if (Randomizer.nextInt(100) < success) {
                Equip template = (Equip) ii.getEquipById(toScroll.getItemId());
                toScroll.setStr(template.getStr());
                toScroll.setDex(template.getDex());
                toScroll.setInt(template.getInt());
                toScroll.setLuk(template.getLuk());
                toScroll.setAcc(template.getAcc());
                toScroll.setAvoid(template.getAvoid());
                toScroll.setSpeed(template.getSpeed());
                toScroll.setJump(template.getJump());
                toScroll.setEnhance(template.getEnhance());
                toScroll.setItemEXP(template.getItemEXP());
                toScroll.setHp(template.getHp());
                toScroll.setMp(template.getMp());
                toScroll.setLevel(template.getLevel());
                toScroll.setWatk(template.getWatk());
                toScroll.setMatk(template.getMatk());
                toScroll.setWdef(template.getWdef());
                toScroll.setMdef(template.getMdef());
                toScroll.setUpgradeSlots(template.getUpgradeSlots());
                toScroll.setViciousHammer(template.getViciousHammer());
                toScroll.setIncSkill(template.getIncSkill());
                if (ItemFlag.PROTECT.check(oldFlag)) { //裝備保護
                    toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.PROTECT.getValue()));
                }
                if (ItemFlag.RECOVERY.check(oldFlag)) { //卷軸保護
                    toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.RECOVERY.getValue()));
                }
                if (scroll.getItemId() != 5064200) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, scroll.getPosition(), (short) 1, false, false);
                }
                c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, false, false));
                chr.getMap().broadcastMessage(chr, CField.getScrollEffect(c.getPlayer().getId(), Equip.ScrollResult.SUCCESS, legendarySpirit ? true : false, false), true);//回真成功
            } else {
                if (scroll.getItemId() != 5064200) {
                    if (ItemFlag.RECOVERY.check(oldFlag)) { //卷軸保護
                        toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.RECOVERY.getValue()));
                        chr.dropMessage(5, "由於卷軸保護的效果，卷軸" + ii.getName(scroll.getItemId()) + "沒有損壞。");
                    } else {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, scroll.getPosition(), (short) 1, false, false);
                    }
                }
                if (Randomizer.nextInt(100) < (100 - success)) {
                    if (ItemFlag.PROTECT.check(oldFlag)) {//裝備保護
                        toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.PROTECT.getValue()));
                        c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, false, false));
                        chr.getMap().broadcastMessage(chr, CField.getScrollEffect(c.getPlayer().getId(), Equip.ScrollResult.FAIL, legendarySpirit ? true : false, false), true);//回真失敗
                    } else {
                        c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, true, false));
                        if (dst < 0) {
                            chr.getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
                        } else {
                            chr.getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
                        }
                        chr.getMap().broadcastMessage(chr, CField.getScrollEffect(c.getPlayer().getId(), Equip.ScrollResult.CURSE, legendarySpirit ? true : false, false), true);//裝備被破壞
                    }
                } else {
                    if (ItemFlag.PROTECT.check(oldFlag)) {//裝備保護
                        toScroll.setFlag((short) (toScroll.getFlag() - ItemFlag.PROTECT.getValue()));
                    }
                    c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, false, false));
                    chr.getMap().broadcastMessage(chr, CField.getScrollEffect(c.getPlayer().getId(), Equip.ScrollResult.FAIL, legendarySpirit ? true : false, false), true);//回真失敗
                }
            }
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }
        if (scroll.getItemId() == 2049615 || scroll.getItemId() == 2049616 || scroll.getItemId() == 2049618) {
            c.getSession().write(CWvsContext.enableActions());
            return false;
        }
        if (!GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() < 1) {
                if (legendarySpirit) {
                    c.getPlayer().getMap().broadcastMessage(CField.getScrollEffect(c.getPlayer().getId(), Equip.ScrollResult.FAIL, legendarySpirit, false));
                }
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("Test 3");
                return false;
            }
        } else if (GameConstants.isEquipScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() >= 1 || toScroll.getEnhance() >= 100 || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("Test 4");
                return false;
            }
        } else if (GameConstants.isPotentialScroll(scroll.getItemId())) {
            final boolean isEpic = scroll.getItemId() / 100 == 20497;
            if ((!isEpic && toScroll.getState() >= 1) || (isEpic && toScroll.getState() >= 18) || (toScroll.getLevel() == 0 && toScroll.getUpgradeSlots() == 0 && toScroll.getItemId() / 10000 != 135 && !isEpic) || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("Test 5");
                return false;
            }
        } else if (GameConstants.isSpecialScroll(scroll.getItemId())) {
            if (ii.isCash(toScroll.getItemId()) || toScroll.getEnhance() >= 8) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("Test 6");
                return false;
            }
        }
        if (!GameConstants.canScroll(toScroll.getItemId()) && !GameConstants.isChaosScroll(toScroll.getItemId())) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("Test 7");
            return false;
        }
        if ((GameConstants.isCleanSlate(scroll.getItemId()) || GameConstants.isTablet(scroll.getItemId()) || GameConstants.isGeneralScroll(scroll.getItemId()) || GameConstants.isChaosScroll(scroll.getItemId())) && (vegas > 0 || ii.isCash(toScroll.getItemId()))) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("Test 8");
            return false;
        }
        if (GameConstants.isTablet(scroll.getItemId()) && toScroll.getDurability() < 0) { //not a durability item
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("Test 9");
            return false;
        } else if ((!GameConstants.isTablet(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId())) && toScroll.getDurability() >= 0) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("Test 10");
            return false;
        }
        Item wscroll = null;

        // Anti cheat and validation
        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (scrollReqs != null && scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
            c.getSession().write(InventoryPacket.getInventoryFull());
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("Test 11");
            return false;
        }

        if (whiteScroll) {
            wscroll = chr.getInventory(MapleInventoryType.USE).findById(2340000);
            if (wscroll == null) {
                whiteScroll = false;
            }
        }
        if ((GameConstants.isTablet(scroll.getItemId()) || GameConstants.isGeneralScroll(scroll.getItemId())) && !(toScroll.getItemId() / 10000 == 166)) {
            switch (scroll.getItemId() % 1000 / 100) {
                case 0: //1h
                    if (GameConstants.isTwoHandeds(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        c.getSession().write(CWvsContext.enableActions());
                        System.out.println("Test 12");
                        return false;
                    }
                    break;
                case 1: //2h
                    if (!GameConstants.isTwoHandeds(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        c.getSession().write(CWvsContext.enableActions());
                        System.out.println("Test 13");
                        return false;
                    }
                    break;
                case 2: //armor
                    if (GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        c.getSession().write(CWvsContext.enableActions());
                        System.out.println("Test 14");
                        return false;
                    }
                    break;
                case 3: //accessory
                    if (!GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        c.getSession().write(CWvsContext.enableActions());
                        System.out.println("Test 15");
                        return false;
                    }
                    break;
            }
        } else if (!GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId()) && !GameConstants.isSpecialScroll(scroll.getItemId())) {
            if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
                if (toScroll.getAndroid2() == false) {
                    c.getSession().write(CWvsContext.enableActions());
                    System.out.println("Test 16");
                    return false;
                }
            }
        }
        if (GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isAccessory(toScroll.getItemId())) {
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("Test 17");
            return false;
        }
        if (scroll.getQuantity() <= 0) {
            c.getSession().write(CWvsContext.enableActions());
            System.out.println("Test 18");
            return false;
        }

        if (legendarySpirit && vegas == 0) {
            if (chr.getSkillLevel(SkillFactory.getSkill(chr.getStat().getSkillByJob(1003, chr.getJob()))) <= 0) {
                c.getSession().write(CWvsContext.enableActions());
                System.out.println("Test 19");
                return false;
            }
        }
        // Scroll Success/ Failure/ Curse
        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll, whiteScroll, chr, vegas);
        Equip.ScrollResult scrollSuccess;
        if (scrolled == null) {
            scrollSuccess = Equip.ScrollResult.CURSE;
            if (ItemFlag.PROTECT.check(toScroll.getFlag())) {//scroll protection
                RECOVERY = true;
            }
        } else if (scrolled.getLevel() > oldLevel || scrolled.getEnhance() > oldEnhance || scrolled.getState() > oldState || scrolled.getFlag() > oldFlag) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else if ((GameConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() > oldSlots)) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else {
            scrollSuccess = Equip.ScrollResult.FAIL;
            if (ItemFlag.RECOVERY.check(toScroll.getFlag())) {
                RECOVERY = true;
            }
        }
        // Update
        if (RECOVERY) {
            chr.dropMessage(5, "Due to the effect of scroll protection, the scroll " + ii.getName(scroll.getItemId()) + " is not damaged.");
        } else {
            chr.getInventory(GameConstants.getInventoryType(scroll.getItemId())).removeItem(scroll.getPosition(), (short) 1, false);
        }
        if (whiteScroll) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
        } else if (scrollSuccess == Equip.ScrollResult.FAIL && scrolled.getUpgradeSlots() < oldSlots && c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000) != null) {
            chr.setScrolledPosition(scrolled.getPosition());
            if (vegas == 0) {
                c.getSession().write(CWvsContext.pamSongUI());
            }
        }

        // Handle to remove flag attributes
        short newFlag = toScroll.getFlag();
        if (ItemFlag.PROTECT.check(oldFlag) && scrollSuccess == Equip.ScrollResult.CURSE) {
            newFlag -= ItemFlag.PROTECT.getValue();
        }
        if (ItemFlag.SAFETY.check(oldFlag)) {
            newFlag -= ItemFlag.SAFETY.getValue();
        }
        if (ItemFlag.RECOVERY.check(oldFlag)) {
            newFlag -= ItemFlag.SAFETY.getValue();
        }
        toScroll.setFlag(newFlag);

        if (scrollSuccess == Equip.ScrollResult.CURSE) {
            boolean isDestroyItem = !RECOVERY;
            c.getSession().write(InventoryPacket.scrolledItem(scroll, toScroll, isDestroyItem, false));
            if (isDestroyItem) {
                if (dst < 0) {
                    chr.getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
                } else {
                    chr.getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
                }
            }
        } else if (vegas == 0) {
            c.getSession().write(InventoryPacket.scrolledItem(scroll, scrolled, false, false));
        }

        chr.getMap().broadcastMessage(chr, CField.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit, whiteScroll), vegas == 0);
        //addToScrollLog(chr.getAccountID(), chr.getId(), scroll.getItemId(), itemID, oldSlots, (byte)(scrolled == null ? -1 : scrolled.getUpgradeSlots()), oldVH, scrollSuccess.name(), whiteScroll, legendarySpirit, vegas);
        // equipped item was scrolled and changed
        if (dst < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE) && vegas == 0) {
            chr.equipChanged();
        }
        return true;
    }

    public static void UseMagicWheel(LittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        if (action == 0x02) {//시작
            int ivtype = slea.readInt();
            byte slot = (byte) slea.readInt();
            int itemid = slea.readInt();
            int type = itemid == 4400001 ? 1 : itemid == 4400002 ? 2 : 0;
            if (itemid == 4400000 || itemid == 4400001 || itemid == 4400002) {//支援道具ID：4400000
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, (byte) slot, (short) 1, false);
                if (c.getPlayer().getInventory(MapleInventoryType.USE).getNextFreeSlot() < 1
                        || c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() < 1
                        || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNextFreeSlot() < 1
                        || c.getPlayer().getInventory(MapleInventoryType.CASH).getNextFreeSlot() < 1
                        || c.getPlayer().getInventory(MapleInventoryType.ETC).getNextFreeSlot() < 1) {
                    c.getSession().write(CWvsContext.magicWheelMessage((byte) 7));
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                List<Integer> items = new ArrayList<Integer>();

                Connection con = null;
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    con = DatabaseConnection.getConnection();
                    ps = con.prepareStatement("SELECT * FROM `wheeldata` WHERE `type` = ? ORDER BY RAND() LIMIT 10");
                    ps.setInt(1, type);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        items.add(rs.getInt("itemid"));
                    }

                } catch (SQLException ex) {
                    ex.printStackTrace();
                } finally {
                    if (ps != null) {
                        try {
                            ps.close();
                        } catch (Exception e) {
                        }
                    }
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (Exception e) {
                        }
                    }
                }
                MagicWheel mw = new MagicWheel(items);
                c.getPlayer().setMagicWheel(mw);
                c.getSession().write(CWvsContext.magicWheelStart(items, mw.getUniqueId(), mw.getRandom()));
            } else {
                c.getSession().write(CWvsContext.magicWheelMessage((byte) 7));
                c.getSession().write(CWvsContext.enableActions());
            }
        } else if (action == 0x04) { //종료
            String uniqueid = slea.readMapleAsciiString();
            MagicWheel mw = c.getPlayer().getMagicWheel();
            MapleInventoryManipulator.addById(c, mw.getItemId(mw.getRandom()), (short) 1, "");
            c.getSession().write(CWvsContext.magicWheelMessage((byte) 5));
            c.getPlayer().setMagicWheel(null);
            // c.getPlayer().setMagicWheel(mw);
        }
    }

    public static final boolean UseSkillBook(final byte slot, final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || chr.hasBlockedInventory()) {
            return false;
        }
        final Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getEquipStats(toUse.getItemId());
        if (skilldata == null) { // Hacking or used an unknown item
            return false;
        }
        boolean canuse = false, success = false;
        int skill = 0, maxlevel = 0;

        final Integer SuccessRate = skilldata.get("success");
        final Integer ReqSkillLevel = skilldata.get("reqSkillLevel");
        final Integer MasterLevel = skilldata.get("masterLevel");

        byte i = 0;
        Integer CurrentLoopedSkillId;
        while (true) {
            CurrentLoopedSkillId = skilldata.get("skillid" + i);
            i++;
            if (CurrentLoopedSkillId == null || MasterLevel == null) {
                break; // End of data
            }
            final Skill CurrSkillData = SkillFactory.getSkill(CurrentLoopedSkillId);
            if (CurrSkillData != null && CurrSkillData.canBeLearnedBy(chr.getJob()) && (ReqSkillLevel == null || chr.getSkillLevel(CurrSkillData) >= ReqSkillLevel) && chr.getMasterLevel(CurrSkillData) < MasterLevel) {
                canuse = true;
                if (SuccessRate == null || Randomizer.nextInt(100) <= SuccessRate) {
                    success = true;
                    chr.changeSingleSkillLevel(CurrSkillData, chr.getSkillLevel(CurrSkillData), (byte) (int) MasterLevel);
                } else {
                    success = false;
                }
                MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(itemId), slot, (short) 1, false);
                break;
            }
        }
        c.getPlayer().getMap().broadcastMessage(CWvsContext.useSkillBook(chr, skill, maxlevel, canuse, success));
        c.getSession().write(CWvsContext.enableActions());
        return canuse;
    }

    public static final void UseCatchItem(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMap map = chr.getMap();

        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null
                && !chr.hasBlockedInventory() && itemid / 10000 == 227
                && MapleItemInformationProvider.getInstance().getCardMobId(itemid) == mob.getId()) {
            if (!MapleItemInformationProvider.getInstance().isMobHP(itemid) || mob.getHp() <= mob.getMobMaxHp() / 2) {
                map.broadcastMessage(MobPacket.catchMonster(mob.getObjectId(), itemid, (byte) 1));
                map.killMonster(mob, chr, true, false, (byte) 1);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, false);
                if (MapleItemInformationProvider.getInstance().getCreateId(itemid) > 0) {
                    MapleInventoryManipulator.addById(c, MapleItemInformationProvider.getInstance().getCreateId(itemid),
                            (short) 1, "Catch item " + itemid + " on " + FileoutputUtil.CurrentReadable_Date());
                }
            } else {
                map.broadcastMessage(MobPacket.catchMonster(mob.getObjectId(), itemid, (byte) 0));
                c.getSession().write(CWvsContext.catchMob(mob.getId(), itemid, (byte) 0));
            }
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void UseMountFood(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt(); // 2260000 usually
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMount mount = chr.getMount();

        if (itemid / 10000 == 226 && toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid
                && mount != null && !c.getPlayer().hasBlockedInventory()) {
            final int fatigue = mount.getFatigue();

            boolean levelup = false;
            mount.setFatigue((byte) -30);

            if (fatigue > 0) {
                mount.increaseExp();
                final int level = mount.getLevel();
                if (level < 30 && mount.getExp() >= GameConstants.getMountExpNeededForLevel(level + 1)) {
                    mount.setLevel((byte) (level + 1));
                    levelup = true;
                }
            }
            chr.getMap().broadcastMessage(CWvsContext.updateMount(chr, levelup));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void UseScriptedNPCItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        long expiration_days = 0;
        int mountid = 0;
        int npc = 9010000;
        String item = "" + itemId;

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && !chr.hasBlockedInventory() && !chr.inPVP()) {
            switch (toUse.getItemId()) {
                case 2430692: //星岩箱子
                    if (c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430692) >= 1) {
                            final int rank = Randomizer.nextInt(100) < 30 ? (Randomizer.nextInt(100) < 4 ? 2 : 1) : 0;
                            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                            final List<StructItemOption> pots = new LinkedList<StructItemOption>(ii.getAllSocketInfo(rank).values());
                            int newId = 0;
                            while (newId == 0) {
                                StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
                                if (pot != null) {
                                    newId = pot.opID;
                                }
                            }
                            if (MapleInventoryManipulator.checkSpace(c, newId, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, newId, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getSession().write(InfoPacket.getShowItemGain(newId, (short) 1, true));
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "You do not have a Nebulite Box.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 2430144: //秘密的技能書
                    final int itemid = Randomizer.nextInt(373) + 2290000;
                    if (MapleItemInformationProvider.getInstance().itemExists(itemid) && !MapleItemInformationProvider.getInstance().getName(itemid).contains("Special") && !MapleItemInformationProvider.getInstance().getName(itemid).contains("Event")) {
                        MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    }
                    break;
                case 2430370: //秘密配方
                    final int scroll = Randomizer.nextInt(2293) + 2510000;
                    if (MapleItemInformationProvider.getInstance().itemExists(scroll) && !MapleItemInformationProvider.getInstance().getName(scroll).contains("Special") && !MapleItemInformationProvider.getInstance().getName(scroll).contains("Event")) {
                        MapleInventoryManipulator.addById(c, scroll, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    }
                    break;
                case 2430327: //pachinko
                    mountid = 1130;
                    expiration_days = -1;
                    break;
                case 2430328: //pachinko
                    mountid = 1130;
                    expiration_days = 90;
                    break;
                default:
                    NPCScriptManager.getInstance().startItemScript(c, npc, item); //maple admin as default npc
                    break;
            }
        }
        if (mountid > 0) {
            mountid = c.getPlayer().getStat().getSkillByJob(mountid, c.getPlayer().getJob());
            final int fk = GameConstants.getMountItem(mountid, c.getPlayer());
            if (GameConstants.GMS && fk > 0 && mountid < 80001000) { //TODO JUMP
                for (int i = 80001001; i < 80001999; i++) {
                    final Skill skill = SkillFactory.getSkill(i);
                    if (skill != null && GameConstants.getMountItem(skill.getId(), c.getPlayer()) == fk) {
                        mountid = i;
                        break;
                    }
                }
            }
            if (c.getPlayer().getSkillLevel(mountid) > 0) {
                c.getPlayer().dropMessage(5, "You already have this skill.");
            } else if (SkillFactory.getSkill(mountid) == null || GameConstants.getMountItem(mountid, c.getPlayer()) == 0) {
                c.getPlayer().dropMessage(5, "The skill could not be gained.");
            } else if (expiration_days > 0) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(mountid), (byte) 1, (byte) 1, System.currentTimeMillis() + (long) (expiration_days * 24 * 60 * 60 * 1000));
                c.getPlayer().dropMessage(5, "The skill has been attained.");
            }
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void UseSummonBag(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        if (!chr.isAlive() || chr.hasBlockedInventory() || chr.inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId
                && (c.getPlayer().getMapId() < 910000000 || c.getPlayer().getMapId() > 910000022)) {
            final Map<String, Integer> toSpawn = MapleItemInformationProvider.getInstance().getEquipStats(itemId);

            if (toSpawn == null) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            MapleMonster ht = null;
            int type = 0;
            for (Entry<String, Integer> i : toSpawn.entrySet()) {
                if (i.getKey().startsWith("mob") && Randomizer.nextInt(99) <= i.getValue()) {
                    ht = MapleLifeFactory.getMonster(Integer.parseInt(i.getKey().substring(3)));
                    chr.getMap().spawnMonster_sSack(ht, chr.getPosition(), type);
                }
            }
            if (ht == null) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void UseTreasureChest(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        final short slot = slea.readShort();
        final int itemid = slea.readInt();

        final Item toUse = chr.getInventory(MapleInventoryType.ETC).getItem((byte) slot);
        if (toUse == null || toUse.getQuantity() <= 0 || toUse.getItemId() != itemid || chr.hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int reward;
        int keyIDforRemoval = 0;
        String box;

        switch (toUse.getItemId()) {
            case 4280000 -> {
                // Gold box
                reward = RandomRewards.getGoldBoxReward();
                keyIDforRemoval = 5490000;
                box = "Gold";
            }
            case 4280001 -> {
                // Silver box
                reward = RandomRewards.getSilverBoxReward();
                keyIDforRemoval = 5490001;
                box = "Silver";
            }
            default -> {
                // Up to no good
                return;
            }
        }

        // Get the quantity
        int amount = 1;
        switch (reward) {
            case 2000004:
                amount = 200; // Elixir
                break;
            case 2000005:
                amount = 100; // Power Elixir
                break;
        }
        if (chr.getInventory(MapleInventoryType.CASH).countById(keyIDforRemoval) > 0) {
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, reward, (short) amount);

            if (item == null) {
                chr.dropMessage(5,
                        "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, (byte) slot, (short) 1, true);
            MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, keyIDforRemoval, 1, true, false);
            c.getSession().write(InfoPacket.getShowItemGain(reward, (short) amount, true));

            if (GameConstants.gachaponRareItem(item.getItemId()) > 0) {
                World.Broadcast.broadcastSmega(CWvsContext.getGachaponMega(c.getPlayer().getName(), " : got a(n)", item,
                        (byte) 2, "[" + box + " Chest]"));
            }
        } else {
            chr.dropMessage(5,
                    "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    public static final void UseCashItem(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null || c.getPlayer().inPVP()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();

        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1
                || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }

        boolean used = false, cc = false;
        action = new InventoryHandlerAction(slea, c, itemId);
        switch (itemId) {

            case 5043001, 5043000 -> // NPC Teleport Rock
            {
                used = action.NpcTeleportRock();
            }
            case 5040004 -> {
                used = action.UseHyperTeleRock();
            }
            case
             5040002,
             2320000, // The Teleport Rock
             5040000 -> { // The Teleport Rock

                used = action.UseTeleRock();
            }
            case 5450005 -> {
                used = action.sendStorage();
            }
            case 5050000 -> {
                used = action.ApReset();
            }
            case 5220083 -> {
                used = action.StarterPack();
            }
            case 5220084 -> {
                used = action.BoosterPack();
            }
            case 5050001, 5050002, 5050003, 5050004, 5050005, 5050006, 5050007, 5050008, 5050009 -> {
                used = action.SpResetScroll();
            }
            case 5500000 -> // Magic Hourglass 1 day
            {
                used = action.HourGlass1();
            }
            case 5500001 -> // Magic Hourglass 7 day
            {
                used = action.HourGlass7();
            }
            case 5500002 -> { // Magic Hourglass 20 day
                used = action.HourGlass20();
            }
            case 5500005 -> { // Magic Hourglass 50 day
                used = action.HourGlass50();
            }
            case 5500006 -> { // Magic Hourglass 99 day
                used = action.HourGlass99();
            }
            case 5060000 -> {// Item Tag
                used = action.ItemTag();
            }
            case 5680015 -> {
                used = action.FatigueResetDrink();
            }
            case 5534000 -> // Tim's Secret Lab (PRovide potential)
            {
                used = action.TimsSecretLab(toUse);
            }
            case 5062000 -> // Miracle cube
            {
                used = action.MiracleCube(toUse);
            }
            case 5062100, 5062001 -> // 8th Anniversary Cube and Premium miracle cube
            {
                used = action.PremiumMiracleCube(toUse);
            }
            case 5062002 -> {
                used = action.SuperMiracleCube(toUse);
            }
            case 5750000 -> {
                used = action.AlienCube();
            }
            case 5750001 -> {
                used = action.NebuliteDiffuser(toUse);
            }
            case 5521000 -> {
                // Karma // THIS IS Sharing TAG... o_o
                /*
             * final MapleInventoryType type = MapleInventoryType.getByType((byte)
             * slea.readInt()); final Item item =
             * c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
             * 
             * if (item != null && !ItemFlag.KARMA_ACC.check(item.getFlag()) &&
             * !ItemFlag.KARMA_ACC_USE.check(item.getFlag())) { if
             * (MapleItemInformationProvider.getInstance().isShareTagEnabled(item.getItemId(
             * ))) { short flag = item.getFlag(); if (ItemFlag.UNTRADEABLE.check(flag)) {
             * flag -= ItemFlag.UNTRADEABLE.getValue(); } else if (type ==
             * MapleInventoryType.EQUIP) { flag |= ItemFlag.KARMA_ACC.getValue(); } else {
             * flag |= ItemFlag.KARMA_ACC_USE.getValue(); } item.setFlag(flag);
             * c.getPlayer().forceReAddItem_NoUpdate(item, type);
             * c.getSession().write(InventoryPacket.updateSpecialItemUse(item,
             * type.getType(), item.getPosition(), true, c.getPlayer())); used = true; } }
                 */
                used = false;
            }
            case 5520001, 5520000 -> { // Platinium scissor of karma / scissor of karna
                used = action.ScissorOfKarma();
            }
            case 2470000, 5570000 -> {
                used = action.ViciousHammer();
            }
            case 5610001, 5610000 -> { // Vega 60 - Vega 10
                used = action.VegaSpell();
                cc = used;

            }
            case 5060001 -> { // Sealing Lock
                used = action.ItemGuard();
            }
            case 5061000 -> { // Sealing Lock 7 days
                used = action.ItemGuard(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));
            }
            case 5061001 -> { // Sealing Lock 30 days
                used = action.ItemGuard(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000));
            }
            case 5061002 -> { // Sealing Lock 90 days
                used = action.ItemGuard(System.currentTimeMillis() + (90 * 24 * 60 * 60 * 1000));
            }
            case 5061003 -> { // Sealing Lock 365 days
                used = action.ItemGuard(System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000));
            }
            case 5063000 -> {
                used = action.LucksKey();
            }
            case 5064000 -> {
                used = action.ShieldingWard();
            }
            case 5060004, 5060003 -> { // Microwave - peanut
                used = action.Microwave_Peanut();
            }
            case 5070000 -> { // Cheap megaphone isn't working :S
                used = action.CheapMegaphone();
            }
            case 5071000 -> { // Megaphone
                used = action.Megaphone();
            }
            case 5077000 -> { // 3 line Megaphone
                used = action.TripleMegaphone();
            }
            case 5079004 -> { // Heart Megaphone
                used = action.EchoMegaphone();
            }
            case 5073000 -> { // Heart Megaphone
                used = action.HeartMegaphone();
            }
            case 5074000 -> { // Skull Megaphone
                used = action.SkullMegaphone();
            }
            case 5072000 -> {
                used = action.SuperMegaphone();
            }
            case 5076000 -> { // Item Megaphone
                used = action.ItemMegaphone();
            }
            case 5075000, 5075001, 5075002 -> { // MapleTV Heart Messenger
                c.getPlayer().dropMessage(5, "There are no MapleTVs to broadcast the message to.");
                used = false;
            }
            case 5075003, 5075004, 5075005 -> {
                used = action.Megassenger();
            }
            case 5090100, 5090000 -> { // Note
                used = action.Note();
            }
            case 5100000 -> { // Congratulatory Song
                c.getPlayer().getMap().broadcastMessage(CField.musicChange("Jukebox/Congratulation"));
                used = true;
            }
            case 5190001, 5190002, 5190003, 5190004, 5190005, 5190006, 5190007, 5190008, 5190000 -> { // Pet Flags
                used = action.PetFlags();
            }
            case 5191001, 5191002, 5191003, 5191004, 5191000 -> { // Pet Flags
                used = action.PetFlag2();
            }
            case 5501001, 5501002 -> { // expiry mount
                used = action.KentasMagicRope();

            }
            case 5170000 -> { // Pet name change
                used = action.PetNameTag();

            }
            case 5700000 -> {
                used = action.AndroidNamingCoupon();
            }
            case 5240000, 5240001, 5240002, 5240003, 5240004, 5240005, 5240006, 5240007, 5240008, 5240009, 5240010, 5240011, 5240012, 5240013, 5240014, 5240015, 5240016, 5240017, 5240018, 5240019, 5240020, 5240021, 5240022, 5240023, 5240024, 5240025, 5240026, 5240027, 5240029, 5240030, 5240031, 5240032, 5240033, 5240034, 5240035, 5240036, 5240037, 5240038, 5240039, 5240040, 5240028 -> {
                used = action.PetSnacks();
            }
            case 5230001, 5230000 -> {
                // Rookie owl of minerva / Owl of Minerva
                used = action.OwlOfMinerva();
            }
            case 5281001, 5280001, 5281000 -> {// Floral Scent /[Not exist?] /Passed gas
                used = action.FloralScent_PassedGas();
            }

            case 5370001, 5370000 -> { // Chalkboard
                used = action.Chalkboard();
            }
            case 5079000, 5079001, 5390007, 5390008, 5390009, 5390000, 5390001, 5390002, 5390003, 5390004, 5390005, 5390006 -> {
                // Goal, soccer, Friend finder, Diablo, Cloud 9, Loveholic, cute tiger, roaring
                used = action.xMessenger();
            }
            case 5452001, 5450003, 5450000 -> { // Miu Miu The Rookie Travelling Merchant, Miu Miu The Travelling Merchant
                used = action.MiuMiuTravelingMerchant();
            }
            case 5300000, 5300001, 5300002 -> { // Cash morphs: Oinker, Zeta, Fungus
                used = action.CashMorphs();
            }
            case 5041000,5041001,5041002, 5041003,5041004,5041005,5041006,5041007, 5040001, 5040003, 5040006, 5040007, 5040008 -> {
                used = action.UseTeleRockDay();
            }
            default -> {
                used = action.DefaultActionForCashItem(slot);
            }
        }
        // NPC Teleport Rock
        // The Teleport Rock
        // VIP Teleport Rock
        // The Teleport Rock
        // SP Reset (1st job)
        // SP Reset (2nd job)
        // SP Reset (3rd job)
        // SP Reset (4th job)
        // evan sp resets
        // p.karma
        // MapleTV Messenger
        // MapleTV Star Messenger
        // Wedding Invitation Card
        // idk, but probably
        // Gas Skill
        // Diablo Messenger
        // Cloud 9 Messenger
        // Loveholic Messenger
        // New Year Megassenger 1
        // New Year Megassenger 2
        // Cute Tiger Messenger

        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false, true);
        }
        c.getSession().write(CWvsContext.enableActions());
        if (cc) {
            if (!c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null
                    || FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
                c.getPlayer().dropMessage(1, "Auto relog failed.");
                return;
            }
            c.getPlayer().dropMessage(5, "Auto relogging. Please wait.");
            c.getPlayer().fakeRelog();
            if (c.getPlayer().getScrolledPosition() != 0) {
                c.getSession().write(CWvsContext.pamSongUI());
            }
        }
    }

    public static final void Pickup_Player(final LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer().hasBlockedInventory()) { // hack
            return;
        }
        chr.updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        slea.skip(1); // or is this before tick?
        final Point Client_Reportedpos = slea.readPos();
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final MapleMapItem mapitem = (MapleMapItem) ob;
        final Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (mapitem.getQuest() > 0 && chr.getQuestStatus(mapitem.getQuest()) != 1) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0)
                    || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId()
                    && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            final double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
            if (Distance > 5000 && (mapitem.getMeso() > 0 || mapitem.getItemId() != 4001025)) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_CLIENT, String.valueOf(Distance));
            } else if (chr.getPosition().distanceSq(mapitem.getPosition()) > 640000.0) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_SERVER);
            }
            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                    final List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if (m != null && m.getId() != chr.getId()) {
                            toGive.add(m);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        int mesos = splitMeso / toGive.size()
                                + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0) : 0);
                        if (mapitem.getDropper() instanceof MapleMonster && m.getStat().incMesoProp > 0) {
                            mesos += Math.floor((m.getStat().incMesoProp * mesos) / 100.0f);
                        }
                        m.gainMeso(mesos, true);
                    }
                    int mesos = mapitem.getMeso() - splitMeso;
                    if (mapitem.getDropper() instanceof MapleMonster && chr.getStat().incMesoProp > 0) {
                        mesos += Math.floor((chr.getStat().incMesoProp * mesos) / 100.0f);
                    }
                    chr.gainMeso(mesos, true);
                } else {
                    int mesos = mapitem.getMeso();
                    if (mapitem.getDropper() instanceof MapleMonster && chr.getStat().incMesoProp > 0) {
                        mesos += Math.floor((chr.getStat().incMesoProp * mesos) / 100.0f);
                    }
                    chr.gainMeso(mesos, true);
                }
                removeItem(chr, mapitem, ob);
            } else if (ItemId.isNxCard(mapitem.getItemId())) {
                int nxGain = mapitem.getItemId() == ItemId.NX_CARD_100 ? 100 : 250;
                c.getPlayer().modifyCSPoints(1, nxGain, true);
                removeItem(chr, mapitem, ob);
            } else {
                if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId())) {
                    c.getSession().write(CWvsContext.enableActions());
                    c.getPlayer().dropMessage(5, "This item cannot be picked up.");
                } else if (c.getPlayer().inPVP() && Integer
                        .parseInt(c.getPlayer().getEventInstance().getProperty("ice")) == c.getPlayer().getId()) {
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.getShowInventoryFull());
                    c.getSession().write(CWvsContext.enableActions());
                } else if (useItem(c, mapitem.getItemId())) {
                    removeItem(c.getPlayer(), mapitem, ob);
                    // another hack
                    if (mapitem.getItemId() / 10000 == 291) {
                        c.getPlayer().getMap().broadcastMessage(CField.getCapturePosition(c.getPlayer().getMap()));
                        c.getPlayer().getMap().broadcastMessage(CField.resetCapture());
                    }
                } else if (mapitem.getItemId() / 10000 != 291 && MapleInventoryManipulator.checkSpace(c,
                        mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                    if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                        c.setMonitored(true); // hack check
                    }
                    MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true,
                            mapitem.getDropper() instanceof MapleMonster);
                    removeItem(chr, mapitem, ob);
                } else {
                    c.getSession().write(InventoryPacket.getInventoryFull());
                    c.getSession().write(InventoryPacket.getShowInventoryFull());
                    c.getSession().write(CWvsContext.enableActions());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static final void Pickup_Pet(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().inPVP()) { // hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        final byte petz = (byte) (GameConstants.GMS ? (c.getPlayer().getPetIndex((int) slea.readLong()))
                : slea.readInt());
        final MaplePet pet = chr.getPet(petz);
        slea.skip(1); // [4] Zero, [4] Seems to be tickcount, [1] Always zero
        chr.updateTick(slea.readInt());
        final Point Client_Reportedpos = slea.readPos();
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null || pet == null) {
            return;
        }
        final MapleMapItem mapitem = (MapleMapItem) ob;
        final Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(InventoryPacket.getInventoryFull());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && mapitem.isPlayerDrop()) {
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0)
                    || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId()
                    && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            final double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
            if (Distance > 10000 && (mapitem.getMeso() > 0 || mapitem.getItemId() != 4001025)) {
                chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_CLIENT, String.valueOf(Distance));
            } else if (pet.getPos().distanceSq(mapitem.getPosition()) > 640000.0) {
                chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_SERVER);

            }

            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                    final List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if (m != null && m.getId() != chr.getId()) {
                            toGive.add(m);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        m.gainMeso(splitMeso / toGive.size()
                                + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0) : 0), true);
                    }
                    chr.gainMeso(mapitem.getMeso() - splitMeso, true);
                } else {
                    chr.gainMeso(mapitem.getMeso(), true);
                }
                removeItem_Pet(chr, mapitem, petz);
            } else if (ItemId.isNxCard(mapitem.getItemId())) {
                int nxGain = mapitem.getItemId() == ItemId.NX_CARD_100 ? 100 : 250;
                c.getPlayer().modifyCSPoints(1, nxGain, true);
                removeItem_Pet(chr, mapitem, petz);
            } else {
                if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId())
                        || mapitem.getItemId() / 10000 == 291) {
                    c.getSession().write(CWvsContext.enableActions());
                } else if (useItem(c, mapitem.getItemId())) {
                    removeItem_Pet(chr, mapitem, petz);
                } else if (MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(),
                        mapitem.getItem().getOwner())) {
                    if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                        c.setMonitored(true); // hack check
                    }
                    MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true,
                            mapitem.getDropper() instanceof MapleMonster);
                    removeItem_Pet(chr, mapitem, petz);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static final boolean useItem(final MapleClient c, final int id) {
        if (GameConstants.isUse(id)) { // TO prevent caching of everything, waste of mem
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleStatEffect eff = ii.getItemEffect(id);
            if (eff == null) {
                return false;
            }
            // must hack here for ctf
            if (id / 10000 == 291) {
                boolean area = false;
                for (Rectangle rect : c.getPlayer().getMap().getAreas()) {
                    if (rect.contains(c.getPlayer().getTruePosition())) {
                        area = true;
                        break;
                    }
                }
                if (!c.getPlayer().inPVP() || (c.getPlayer().getTeam() == (id - 2910000) && area)) {
                    return false; // dont apply the consume
                }
            }
            final int consumeval = eff.getConsume();

            if (consumeval > 0) {
                consumeItem(c, eff);
                consumeItem(c, ii.getItemEffectEX(id));
                c.getSession().write(InfoPacket.getShowItemGain(id, (byte) 1));
                return true;
            }
        }
        return false;
    }

    public static final void consumeItem(final MapleClient c, final MapleStatEffect eff) {
        if (eff == null) {
            return;
        }
        if (eff.getConsume() == 2) {
            if (c.getPlayer().getParty() != null && c.getPlayer().isAlive()) {
                c.getPlayer().getParty().getMembers().stream()
                        .map(pc -> c.getPlayer().getMap().getCharacterById(pc.getId()))
                        .filter(chr -> (chr != null && chr.isAlive())).forEachOrdered(chr -> {
                    eff.applyTo(chr);
                });
            } else {
                eff.applyTo(c.getPlayer());
            }
        } else if (c.getPlayer().isAlive()) {
            eff.applyTo(c.getPlayer());
        }
    }

    public static final void removeItem_Pet(final MapleCharacter chr, final MapleMapItem mapitem, int pet) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(CField.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), pet));
        chr.getMap().removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static final void removeItem(final MapleCharacter chr, final MapleMapItem mapitem,
            final MapleMapObject ob) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(CField.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()),
                mapitem.getPosition());
        chr.getMap().removeMapObject(ob);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    public static final void OwlMinerva(final LittleEndianAccessor slea, final MapleClient c) {
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && itemid == 2310000
                && !c.getPlayer().hasBlockedInventory()) {
            final int itemSearch = slea.readInt();
            final List<HiredMerchant> hms = c.getChannelServer().searchMerchant(itemSearch);
            if (hms.size() > 0) {
                c.getSession().write(CWvsContext.getOwlSearched(itemSearch, hms));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, false);
            } else {
                c.getPlayer().dropMessage(1, "Unable to find the item.");
            }
        }
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void Owl(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().haveItem(5230000, 1, true, false) || c.getPlayer().haveItem(2310000, 1, true, false)) {
            if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022) {
                c.getSession().write(CWvsContext.getOwlOpen());
            } else {
                c.getPlayer().dropMessage(5, "This can only be used inside the Free Market.");
                c.getSession().write(CWvsContext.enableActions());
            }
        }
    }

    public static final int OWL_ID = 2; // don't change. 0 = owner ID, 1 = store ID, 2 = object ID

    public static final void OwlWarp(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(CWvsContext.getOwlMessage(4));
            return;
        } else if (c.getPlayer().getTrade() != null) {
            c.getSession().write(CWvsContext.getOwlMessage(7));
            return;
        }
        if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022
                && !c.getPlayer().hasBlockedInventory()) {
            final int id = slea.readInt();
            final int map = slea.readInt();
            if (map >= 910000001 && map <= 910000022) {
                c.getSession().write(CWvsContext.getOwlMessage(0));
                final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(map);
                c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                HiredMerchant merchant = null;
                List<MapleMapObject> objects;
                switch (OWL_ID) {
                    case 0 -> {
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop iMaplePlayerShop) {
                                final IMaplePlayerShop ips = iMaplePlayerShop;
                                if (ips instanceof HiredMerchant hiredMerchant) {
                                    final HiredMerchant merch = hiredMerchant;
                                    if (merch.getOwnerId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    case 1 -> {
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop iMaplePlayerShop) {
                                final IMaplePlayerShop ips = iMaplePlayerShop;
                                if (ips instanceof HiredMerchant hiredMerchant) {
                                    final HiredMerchant merch = hiredMerchant;
                                    if (merch.getStoreId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    default -> {
                        final MapleMapObject ob = mapp.getMapObject(id, MapleMapObjectType.HIRED_MERCHANT);
                        if (ob instanceof IMaplePlayerShop iMaplePlayerShop) {
                            final IMaplePlayerShop ips = iMaplePlayerShop;
                            if (ips instanceof HiredMerchant hiredMerchant) {
                                merchant = hiredMerchant;
                            }
                        }
                    }
                }
                if (merchant != null) {
                    if (merchant.isOwner(c.getPlayer())) {
                        merchant.setOpen(false);
                        merchant.removeAllVisitors((byte) 16, (byte) 0);
                        c.getPlayer().setPlayerShop(merchant);
                        c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                    } else {
                        if (!merchant.isOpen() || !merchant.isAvailable()) {
                            c.getPlayer().dropMessage(1,
                                    "The owner of the store is currently undergoing store maintenance. Please try again in a bit.");
                        } else {
                            if (merchant.getFreeSlot() == -1) {
                                c.getPlayer().dropMessage(1, "You can't enter the room due to full capacity.");
                            } else if (merchant.isInBlackList(c.getPlayer().getName())) {
                                c.getPlayer().dropMessage(1, "You may not enter this store.");
                            } else {
                                c.getPlayer().setPlayerShop(merchant);
                                merchant.addVisitor(c.getPlayer());
                                c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                            }
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(1, "The room is already closed.");
                }
            } else {
                c.getSession().write(CWvsContext.getOwlMessage(23));
            }
        } else {
            c.getSession().write(CWvsContext.getOwlMessage(23));
        }
    }

    public static final void PamSong(LittleEndianAccessor slea, MapleClient c) {
        final Item pam = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000);
        if (slea.readByte() > 0 && c.getPlayer().getScrolledPosition() != 0 && pam != null && pam.getQuantity() > 0) {
            final MapleInventoryType inv = c.getPlayer().getScrolledPosition() < 0 ? MapleInventoryType.EQUIPPED
                    : MapleInventoryType.EQUIP;
            final Item item = c.getPlayer().getInventory(inv).getItem(c.getPlayer().getScrolledPosition());
            c.getPlayer().setScrolledPosition((short) 0);
            if (item != null) {
                final Equip eq = (Equip) item;
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + 1));
                c.getPlayer().forceReAddItem_Flag(eq, inv);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, pam.getPosition(), (short) 1, true,
                        false);
                c.getPlayer().getMap().broadcastMessage(CField.pamsSongEffect(c.getPlayer().getId()));
            }
        } else {
            c.getPlayer().setScrolledPosition((short) 0);
        }
    }

    public static final void TeleRock(LittleEndianAccessor slea, MapleClient c) {
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 232
                || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        boolean used = action.UseTeleRock();
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(CWvsContext.enableActions());
    }
}
