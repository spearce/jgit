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

/**
 * Base class for a set of object loader classes for packed
 * objects.
 */
abstract class PackedObjectLoader extends ObjectLoader {
	protected final PackFile pack;

	protected final long dataOffset;

	protected int objectType;

	protected int objectSize;

	/**
	 * Constructor for a packed object loader in the specified pack file
	 * at an offset
	 * @param pr pack file
	 * @param offset offset of object within pack file
	 */
	protected PackedObjectLoader(final PackFile pr, final long offset) {
		pack = pr;
		dataOffset = offset;
	}

	public int getType() throws IOException {
		return objectType;
	}

	public long getSize() throws IOException {
		return objectSize;
	}

	/**
	 * @return offset of object data within pack file
	 */
	public long getDataOffset() {
		return dataOffset;
	}

	public final byte[] getBytes() throws IOException {
		final byte[] data = getCachedBytes();
		final byte[] copy = new byte[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		return data;
	}
}
