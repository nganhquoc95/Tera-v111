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
package tools.packet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import client.MapleClient;
import client.MapleCharacter;
import constants.ServerConstants;

import handling.SendPacketOpcode;
import handling.login.LoginServer;
import java.util.LinkedList;
import tools.data.MaplePacketLittleEndianWriter;
import tools.HexTool;
import server.Randomizer;
import tools.Pair;

public class LoginPacket {

    public static final byte[] getHello(final short mapleVersion, final byte[] sendIv, final byte[] recvIv) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(15 + ServerConstants.MAPLE_PATCH.length());

        mplew.writeShort(13 + ServerConstants.MAPLE_PATCH.length()); // length of the packet
        mplew.writeShort(mapleVersion);
        mplew.writeMapleAsciiString(ServerConstants.MAPLE_PATCH);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(8); // 7 = MSEA, 8 = GlobalMS, 5 = Test Server

        return mplew.getPacket();
    }

    public static final byte[] getPing() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);

        mplew.writeShort(SendPacketOpcode.PING.getValue());

        return mplew.getPacket();
    }

    public static byte[] sendGuestTOS() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_LINK.getValue());
        mplew.writeShort(0x100);
        mplew.writeInt(Randomizer.nextInt(999999));
        mplew.writeLong(0);
        mplew.write(new byte[]{(byte) 0x40, (byte) 0xE0, (byte) 0xFD, (byte) 0x3B, (byte) 0x37, (byte) 0x4F, 1});
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.writeInt(0);
        mplew.writeMapleAsciiString("http://maplefags.com");
        return mplew.getPacket();
    }

    public static final byte[] getAuthSuccessRequest(final MapleClient client) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeZeroBytes(6);
        mplew.writeInt(client.getAccID());
        mplew.write(client.getGender());
        mplew.write(client.isGm() ? 0 : 0); // Admin byte - Find, Trade, etc.
        mplew.writeShort(0/*2*/); //0 for new accounts
        mplew.write(0/*client.isGm() ? 1 : 0*/); // Admin byte - Commands
        mplew.writeMapleAsciiString(client.getAccountName());
        mplew.write(3); //0 for new accounts
        mplew.write(0); // quiet ban
        mplew.writeLong(0); // quiet ban time
        mplew.write(1);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis())); //really create date
        mplew.writeInt(4); // Remove the "Select the world you want to play in" since it doesn't fit inside the loginscreen
        mplew.write(1); //1 = pin disabled, 0 = pin enabled
        if (client.isPicEnable()) {
            if (client.getSecondPassword() == null || client.getSecondPassword().length() == 0) {
                mplew.write(0);
            } else {
                mplew.write(1);
            }
        } else {
            mplew.write(2); //2 = no pic at all
        }

        // mplew.write(client.getSecondPassword() == null ? 0 : (client.getSecondPassword().equals("") ? 2 : 1)); //2 = no pic at all
        mplew.writeLong(Randomizer.nextLong());
        return mplew.getPacket();
    }

    public static final byte[] getLoginAUTH() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(13);

        mplew.writeShort(SendPacketOpcode.LOGIN_AUTH.getValue());
        mplew.writeMapleAsciiString("MapLogin2");
        return mplew.getPacket();
    }

    public static final byte[] getLoginFailed(final int reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        // -1/6/8/9 : Trouble logging in
        // 2/3 : Id deleted or blocked
        // 4: Incorrect password
        // 5: Not a registered ID
        // 7: Logged in    
        // 10: Too many requests
        // 11: 20 years older can use
        // 13: Unable to log on as master at IP
        // 14/15: Redirect to nexon + buttons    
        // 16/21: Verify account
        // 17: Selected the wrong gateway
        // 25: Logging in outside service region
        // 23: License agreement
        // 27: Download full client
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(reason);
        mplew.write(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static final byte[] getPermBan(final byte reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeShort(2); // Account is banned
        mplew.writeInt(0);
        mplew.writeShort(reason);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01 00"));

        return mplew.getPacket();
    }

    public static final byte[] getTempBan(final long timestampTill, final byte reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);

        // 99 : You have been blocked for typing in an invalid password or pincode 5 times.
        // 199 : You have been blocked for typing in an invalid password or pincode 10 times.
        // 299 : You have been blocked for typing in an invalid password or pincode more than 10 times.			
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(2);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(reason);
        mplew.writeLong(timestampTill); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601.

        return mplew.getPacket();
    }

    public static final byte[] getGenderChanged(final MapleClient client) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GENDER_SET.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(client.getAccountName());
        mplew.writeMapleAsciiString(String.valueOf(client.getAccID()));

        return mplew.getPacket();
    }

    public static final byte[] getSecondAuthSuccess(final MapleClient client) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_SECOND.getValue());
        mplew.write(0);
        mplew.writeInt(client.getAccID());
        mplew.write(client.getGender());
        mplew.write(client.isGm() ? 0 : 0); // Admin byte - Find, Trade, etc.
        mplew.writeShort(2);
        mplew.write(client.isGm() ? 1 : 0); // Admin byte - Commands
        mplew.writeMapleAsciiString(client.getAccountName());
        mplew.write(3); //0 for new accounts
        mplew.write(0); // quiet ban
        mplew.writeLong(0); // quiet ban time
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis())); //really create date
        mplew.writeInt(4); //idk
        mplew.writeLong(Randomizer.nextLong()); //randomizer.nextLong(), remote hack check.
        mplew.write(1); // a boolean, 1/0
        mplew.write(1); // a boolean, 1/0
        mplew.write(1); // a boolean, 1/0
        mplew.write(1); // a boolean, 1/0

        return mplew.getPacket();
    }

    public static final byte[] deleteCharResponse(final int cid, final int state) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        mplew.writeInt(cid);
        mplew.write(state);

        return mplew.getPacket();
    }

    public static final byte[] secondPwError(final byte mode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        /*
         * 14 - Invalid password
         * 15 - Second password is incorrect
         */
        mplew.writeShort(SendPacketOpcode.SECONDPW_ERROR.getValue());
        mplew.write(0/*mode*/);

        return mplew.getPacket();
    }

    public static byte[] enableRecommended() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENABLE_RECOMMENDED.getValue());
        mplew.writeInt(0); //worldID with most characters
        return mplew.getPacket();
    }

    public static byte[] sendRecommended(int world, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_RECOMMENDED.getValue());
        mplew.write(message != null ? 1 : 0); //amount of messages
        if (message != null) {
            mplew.writeInt(world);
            mplew.writeMapleAsciiString(message);
        }
        return mplew.getPacket();
    }

    public static final byte[] getServerList(final int serverId, final Map<Integer, Integer> channelLoad) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(serverId); // 0 = Aquilla, 1 = bootes, 2 = cass, 3 = delphinus
        final String worldName = LoginServer.getTrueServerName(); //remove the SEA
        mplew.writeMapleAsciiString(worldName);
        mplew.write(LoginServer.getFlag());
        mplew.writeMapleAsciiString(LoginServer.getEventMessage());
        mplew.writeShort(100);
        mplew.writeShort(100);
        mplew.write(0);
        int lastChannel = 1;
        Set<Integer> channels = channelLoad.keySet();
        for (int i = 30; i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        mplew.write(lastChannel);
        int load;
        for (int i = 1; i <= lastChannel; i++) {
            if (channels.contains(i)) {
                load = channelLoad.get(i);
            } else {
                load = 1200;
            }
            mplew.writeMapleAsciiString(worldName + "-" + i);
            mplew.writeInt(load);
            mplew.write(serverId);
            mplew.writeShort(i - 1);
        }
        mplew.writeShort(0); //size: (short x, short y, string msg)
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static final byte[] getEndOfServerList() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(0xFF);

        return mplew.getPacket();
    }

    public static final byte[] getLoginWelcome() {
        List<Pair<String, Integer>> flags = new LinkedList<>();
        flags.add(new Pair<>("20120111", 0));
        flags.add(new Pair<>("returnLegend2", 0));
        return CField.spawnFlags(flags);
    }

    public static final byte[] getServerStatus(final int status) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        /*	 * 0 - Normal
         * 1 - Highly populated
         * 2 - Full*/
        mplew.writeShort(SendPacketOpcode.SERVERSTATUS.getValue());
        mplew.writeShort(status);

        return mplew.getPacket();
    }

    public static final byte[] getChannelSelected() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANNEL_SELECTED.getValue());
        mplew.writeZeroBytes(3);

        return mplew.getPacket();
    }

    public static final byte[] getCharList(MapleClient c, final List<MapleCharacter> chars, int charslots) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHARLIST.getValue());
        mplew.write(0);
        mplew.write(chars.size());
        for (final MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, !chr.isGM() && chr.getLevel() >= 30, false);
        }

        if (c.isPicEnable()) {
            if (c.getSecondPassword() == null || c.getSecondPassword().length() == 0) {
                mplew.write(0);
            } else {
                mplew.write(1);
            }
        } else {
            mplew.write(2);            
        }

        mplew.write(0);
        mplew.writeInt(charslots);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static final byte[] addNewCharEntry(final MapleCharacter chr, final boolean worked) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(worked ? 0 : 1);
        addCharEntry(mplew, chr, false, false);

        return mplew.getPacket();
    }

    public static final byte[] charNameResponse(final String charname, final boolean nameUsed) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);

        return mplew.getPacket();
    }

    private static final void addCharEntry(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr, boolean ranking, boolean viewAll) {
        PacketHelper.addCharStats(mplew, chr);
        PacketHelper.addCharLook(mplew, chr, true);
        if (!viewAll) {
            mplew.write(0);
        }
        mplew.write(ranking ? 1 : 0);
        if (ranking) {
            mplew.writeInt(chr.getRank());
            mplew.writeInt(chr.getRankMove());
            mplew.writeInt(chr.getJobRank());
            mplew.writeInt(chr.getJobRankMove());
        }
    }

    public static byte[] showAllCharacter(int chars) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(1); //bIsChar
        mplew.writeInt(chars);
        mplew.writeInt(chars + (3 - chars % 3)); //rowsize
        return mplew.getPacket();
    }

    public static byte[] showAllCharacterInfo(int worldid, List<MapleCharacter> chars, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(chars.size() == 0 ? 5 : 0); //5 = cannot find any
        mplew.write(worldid);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, true, true);
        }

        if (c.isPicEnable()) {
            if (c.getSecondPassword() == null || c.getSecondPassword().length() == 0) {
                mplew.write(0);
            } else {
                mplew.write(1);
            }
        } else {
            mplew.write(2); // writing 2 here disables PIC
        }

        return mplew.getPacket();
    }

    public static byte[] enableSpecialCreation(int accid, boolean enable) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPECIAL_CREATION.getValue());
        mplew.writeInt(accid);
        mplew.write(enable ? 0 : 1);
        mplew.write(0); // amount of legends created

        return mplew.getPacket();
    }
}
