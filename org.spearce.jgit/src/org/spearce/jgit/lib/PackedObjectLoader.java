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

abstract class PackedObjectLoader extends ObjectLoader {
	protected final PackFile pack;

	protected final long dataOffset;

	protected String objectType;

	protected int objectSize;

	protected PackedObjectLoader(final PackFile pr, final long offset) {
		pack = pr;
		dataOffset = offset;
	}

	public String getType() throws IOException {
		return objectType;
	}

	public long getSize() throws IOException {
		return objectSize;
	}

	public long getDataOffset() {
		return dataOffset;
	}
}
