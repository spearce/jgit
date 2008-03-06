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
	private final int compression;

	CoreConfig(final RepositoryConfig rc) {
		compression = rc.getInt("core", null,
				"compression", Deflater.DEFAULT_COMPRESSION);
	}

	/**
	 * @see ObjectWriter
	 * @return The compression level to use when storing loose objects
	 */
	public int getCompression() {
		return compression;
	}
}
