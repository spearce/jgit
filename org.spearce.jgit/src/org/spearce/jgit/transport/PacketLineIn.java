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
import java.io.InputStream;
import java.util.Arrays;

import org.spearce.jgit.errors.PackProtocolException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.MutableObjectId;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.util.NB;

class PacketLineIn {
	private static final byte fromhex[];

	static {
		fromhex = new byte['f' + 1];
		Arrays.fill(fromhex, (byte) -1);
		for (char i = '0'; i <= '9'; i++)
			fromhex[i] = (byte) (i - '0');
		for (char i = 'a'; i <= 'f'; i++)
			fromhex[i] = (byte) ((i - 'a') + 10);
	}

	static enum AckNackResult {
		/** NAK */
		NAK,
		/** ACK */
		ACK,
		/** ACK + continue */
		ACK_CONTINUE
	}

	private final InputStream in;

	private final byte[] lenbuffer;

	PacketLineIn(final InputStream i) {
		in = i;
		lenbuffer = new byte[4];
	}

	InputStream sideband(final ProgressMonitor pm) {
		return new SideBandInputStream(this, in, pm);
	}

	AckNackResult readACK(final MutableObjectId returnedId) throws IOException {
		final String line = readString();
		if (line.length() == 0)
			throw new PackProtocolException("Expected ACK/NAK, found EOF");
		if ("NAK".equals(line))
			return AckNackResult.NAK;
		if (line.startsWith("ACK ")) {
			returnedId.fromString(line.substring(4, 44));
			if (line.indexOf("continue", 44) != -1)
				return AckNackResult.ACK_CONTINUE;
			return AckNackResult.ACK;
		}
		throw new PackProtocolException("Expected ACK/NAK, got: " + line);
	}

	String readString() throws IOException {
		int len = readLength();
		if (len == 0)
			return "";

		len -= 5; // length header (4 bytes) and trailing LF.

		final byte[] raw = new byte[len];
		NB.readFully(in, raw, 0, len);
		readLF();
		return new String(raw, 0, len, Constants.CHARACTER_ENCODING);
	}

	private void readLF() throws IOException {
		if (in.read() != '\n')
			throw new IOException("Protocol error: expected LF");
	}

	int readLength() throws IOException {
		NB.readFully(in, lenbuffer, 0, 4);
		try {
			int r = fromhex[lenbuffer[0]] << 4;

			r |= fromhex[lenbuffer[1]];
			r <<= 4;

			r |= fromhex[lenbuffer[2]];
			r <<= 4;

			r |= fromhex[lenbuffer[3]];
			if (r < 0)
				throw new ArrayIndexOutOfBoundsException();
			return r;
		} catch (ArrayIndexOutOfBoundsException err) {
			throw new IOException("Invalid packet line header: "
					+ (char) lenbuffer[0] + (char) lenbuffer[1]
					+ (char) lenbuffer[2] + (char) lenbuffer[3]);
		}
	}
}
