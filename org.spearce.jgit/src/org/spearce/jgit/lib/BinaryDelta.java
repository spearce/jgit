/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.lib;

/**
 * Recreate a stream from a base stream and a GIT pack delta.
 * <p>
 * This entire class is heavily cribbed from <code>patch-delta.c</code> in the
 * GIT project. The original delta patching code was written by Nicolas Pitre
 * (&lt;nico@cam.org&gt;).
 * </p>
 */
public class BinaryDelta {
	public static final byte[] apply(final byte[] base, final byte[] delta) {
		int deltaPtr = 0;

		// Length of the base object (a variable length int).
		//
		int baseLen = 0;
		int c, shift = 0;
		do {
			c = delta[deltaPtr++] & 0xff;
			baseLen |= (c & 0x7f) << shift;
			shift += 7;
		} while ((c & 0x80) != 0);
		if (base.length != baseLen)
			throw new IllegalArgumentException("base length incorrect");

		// Length of the resulting object (a variable length int).
		//
		int resLen = 0;
		shift = 0;
		do {
			c = delta[deltaPtr++] & 0xff;
			resLen |= (c & 0x7f) << shift;
			shift += 7;
		} while ((c & 0x80) != 0);

		final byte[] result = new byte[resLen];
		int resultPtr = 0;
		while (deltaPtr < delta.length) {
			final int cmd = delta[deltaPtr++] & 0xff;
			if ((cmd & 0x80) != 0) {
				// Determine the segment of the base which should
				// be copied into the output. The segment is given
				// as an offset and a length.
				//
				int copyOffset = 0;
				if ((cmd & 0x01) != 0)
					copyOffset = delta[deltaPtr++] & 0xff;
				if ((cmd & 0x02) != 0)
					copyOffset |= (delta[deltaPtr++] & 0xff) << 8;
				if ((cmd & 0x04) != 0)
					copyOffset |= (delta[deltaPtr++] & 0xff) << 16;
				if ((cmd & 0x08) != 0)
					copyOffset |= (delta[deltaPtr++] & 0xff) << 24;

				int copySize = 0;
				if ((cmd & 0x10) != 0)
					copySize = delta[deltaPtr++] & 0xff;
				if ((cmd & 0x20) != 0)
					copySize |= (delta[deltaPtr++] & 0xff) << 8;
				if ((cmd & 0x40) != 0)
					copySize |= (delta[deltaPtr++] & 0xff) << 16;
				if (copySize == 0)
					copySize = 0x10000;

				System.arraycopy(base, copyOffset, result, resultPtr, copySize);
				resultPtr += copySize;
			} else if (cmd != 0) {
				// Anything else the data is literal within the delta
				// itself.
				//
				System.arraycopy(delta, deltaPtr, result, resultPtr, cmd);
				deltaPtr += cmd;
				resultPtr += cmd;
			} else {
				// cmd == 0 has been reserved for future encoding but
				// for now its not acceptable.
				//
				throw new IllegalArgumentException("unsupported command 0");
			}
		}

		return result;
	}
}
