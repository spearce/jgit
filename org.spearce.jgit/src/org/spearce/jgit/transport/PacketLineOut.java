/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.transport;

import java.io.IOException;
import java.io.OutputStream;

import org.spearce.jgit.lib.Constants;

class PacketLineOut {
	private final OutputStream out;

	private final byte[] lenbuffer;

	PacketLineOut(final OutputStream i) {
		out = i;
		lenbuffer = new byte[4];
	}

	void writeString(final String s) throws IOException {
		writePacket(Constants.encodeASCII(s));
	}

	void writePacket(final byte[] packet) throws IOException {
		writeLength(packet.length + 4);
		out.write(packet);
	}

	void end() throws IOException {
		writeLength(0);
		flush();
	}

	void flush() throws IOException {
		out.flush();
	}

	private static final byte[] hexchar = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private void writeLength(int w) throws IOException {
		int o = 3;
		while (o >= 0 && w != 0) {
			lenbuffer[o--] = hexchar[w & 0xf];
			w >>>= 4;
		}
		while (o >= 0)
			lenbuffer[o--] = '0';
		out.write(lenbuffer, 0, 4);
	}
}
