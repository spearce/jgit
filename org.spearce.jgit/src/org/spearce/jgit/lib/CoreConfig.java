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

import java.util.zip.Deflater;

/**
 * This class keeps git repository core parameters.
 */
public class CoreConfig {
	private static final int MB = 1024 * 1024;

	private final int compression;

	private final int packedGitWindowSize;

	private final int packedGitLimit;

	private final boolean packedGitMMAP;

	CoreConfig(final RepositoryConfig rc) {
		compression = rc.getInt("core", null, "compression",
				Deflater.DEFAULT_COMPRESSION);
		int win = rc.getInt("core", null, "packedgitwindowsize", 32 * MB);
		if (win < 4096)
			win = 4096;
		if (Integer.bitCount(win) != 1)
			win = Integer.highestOneBit(win);
		packedGitWindowSize = win;
		packedGitLimit = rc.getInt("core", null, "packedgitlimit", 256 * MB);
		packedGitMMAP = rc.getBoolean("core", null, "packedgitmmap", true);
	}

	/**
	 * @see ObjectWriter
	 * @return The compression level to use when storing loose objects
	 */
	public int getCompression() {
		return compression;
	}

	/**
	 * The maximum window size to mmap/read in a burst.
	 *
	 * @return number of bytes in a window.  Always a power of two.
	 */
	public int getPackedGitWindowSize() {
		return packedGitWindowSize;
	}

	/**
	 * The maximum number of bytes to allow in the cache at once.
	 *
	 * @return number of bytes to cache at any time.
	 */
	public int getPackedGitLimit() {
		return packedGitLimit;
	}

	/**
	 * Enable mmap for packfile data access?
	 *
	 * @return true if mmap should be preferred.
	 */
	public boolean isPackedGitMMAP() {
		return packedGitMMAP;
	}
}
