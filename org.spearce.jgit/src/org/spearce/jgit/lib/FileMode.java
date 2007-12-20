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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Constants describing various file modes recognized by GIT.
 * <p>
 * GIT uses a subset of the available UNIX file permission bits. The
 * <code>FileMode</code> class provides access to constants defining the modes
 * actually used by GIT.
 * </p>
 */
public abstract class FileMode {
	/** Mode indicating an entry is a {@link Tree}. */
	@SuppressWarnings("synthetic-access")
	public static final FileMode TREE = new FileMode(040000) {
		public boolean equals(final int modeBits) {
			return (modeBits & 040000) == 040000;
		}
	};

	/** Mode indicating an entry is a {@link SymlinkTreeEntry}. */
	@SuppressWarnings("synthetic-access")
	public static final FileMode SYMLINK = new FileMode(0120000) {
		public boolean equals(final int modeBits) {
			return (modeBits & 020000) == 020000;
		}
	};

	/** Mode indicating an entry is a non-executable {@link FileTreeEntry}. */
	@SuppressWarnings("synthetic-access")
	public static final FileMode REGULAR_FILE = new FileMode(0100644) {
		public boolean equals(final int modeBits) {
			return (modeBits & 0100000) == 0100000 && (modeBits & 0111) == 0;
		}
	};

	/** Mode indicating an entry is an executable {@link FileTreeEntry}. */
	@SuppressWarnings("synthetic-access")
	public static final FileMode EXECUTABLE_FILE = new FileMode(0100755) {
		public boolean equals(final int modeBits) {
			return (modeBits & 0100000) == 0100000 && (modeBits & 0111) != 0;
		}
	};

	private final byte[] octalBytes;

	private final int modeBits;

	private FileMode(int mode) {
		modeBits = mode;
		if (mode != 0) {
			final byte[] tmp = new byte[10];
			int p = tmp.length;

			while (mode != 0) {
				tmp[--p] = (byte) ('0' + (mode & 07));
				mode >>= 3;
			}

			octalBytes = new byte[tmp.length - p];
			for (int k = 0; k < octalBytes.length; k++) {
				octalBytes[k] = tmp[p + k];
			}
		} else {
			octalBytes = new byte[] { '0' };
		}
	}

	/**
	 * Test a file mode for equality with this {@link FileMode} object.
	 *
	 * @param modebits
	 * @return true if the mode bits represent the same mode as this object
	 */
	public abstract boolean equals(final int modebits);

	/**
	 * Copy this mode as a sequence of octal US-ASCII bytes.
	 * <p>
	 * The mode is copied as a sequence of octal digits using the US-ASCII
	 * character encoding. The sequence does not use a leading '0' prefix to
	 * indicate octal notation. This method is suitable for generation of a mode
	 * string within a GIT tree object.
	 * </p>
	 * 
	 * @param os
	 *            stream to copy the mode to.
	 * @throws IOException
	 *             the stream encountered an error during the copy.
	 */
	public void copyTo(final OutputStream os) throws IOException {
		os.write(octalBytes);
	}

	/** Format this mode as an octal string (for debugging only). */
	public String toString() {
		return Integer.toOctalString(modeBits);
	}

	/**
	 * @return The mode bits as an integer.
	 */
	public int getBits() {
		return modeBits;
	}
}
