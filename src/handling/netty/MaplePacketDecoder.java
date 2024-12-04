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
package handling.netty;

import client.MapleClient;
import constants.ServerConstants;
import handling.RecvPacketOpcode;
import handling.login.LoginServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import java.util.Arrays;
import java.util.List;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.MapleCustomEncryption;
import tools.data.input.GenericLittleEndianAccessor;

public class MaplePacketDecoder extends ByteToMessageDecoder {

	public static final AttributeKey<DecoderState> DECODER_STATE_KEY = AttributeKey.newInstance(MaplePacketDecoder.class.getName() + ".STATE");

	public static class DecoderState {

		public int packetlength = -1;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> message) throws Exception {
		final DecoderState decoderState = ctx.channel().attr(DECODER_STATE_KEY).get();
		final MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();

		if (decoderState.packetlength == -1) {
			if (in.readableBytes() >= 4) {
				final int packetHeader = in.readInt();
				if (!client.getReceiveCrypto().checkPacket(packetHeader)) {
					ctx.channel().disconnect();
					return;
				}
				decoderState.packetlength = MapleAESOFB.getPacketLength(packetHeader);
			} else {
				return;
			}
		}
		if (in.readableBytes() >= decoderState.packetlength) {
			final byte decryptedPacket[] = new byte[decoderState.packetlength];
			in.readBytes(decryptedPacket);
			decoderState.packetlength = -1;

			client.getReceiveCrypto().crypt(decryptedPacket);
			MapleCustomEncryption.decryptData(decryptedPacket);
			message.add(decryptedPacket);
			int packetLen = decryptedPacket.length;
			short pHeader = new GenericLittleEndianAccessor(new tools.data.input.ByteArrayByteStream(decryptedPacket)).readShort();
			if (ServerConstants.DEBUG) {
				String pHeaderStr = Integer.toHexString(pHeader).toUpperCase();

				String op = nameOf(pHeader);
				String tab = "";
				for (int i = 4; i > op.length() / 8; i--) {
					tab += "\t";
				}
				String Send = "用戶端發送 " + op + " [" + pHeaderStr + "] (" + packetLen + ")\r\n";
				if (packetLen <= 6000) {
					String SendTo = Send + HexTool.toString(decryptedPacket) + "\r\n" + HexTool.toStringFromAscii(decryptedPacket);
					System.out.println(SendTo + "\r\n");
				}
			}
		}
	}

	public static String nameOf(short value) {
		for (RecvPacketOpcode header : RecvPacketOpcode.values()) {
			if (header.getValue() == value) {
				return header.name();
			}
		}
		return "UNKNOWN";
	}
}