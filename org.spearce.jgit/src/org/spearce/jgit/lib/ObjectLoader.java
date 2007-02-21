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
import java.security.MessageDigest;

public abstract class ObjectLoader {
	private ObjectId objectId;

	public ObjectId getId() throws IOException {
		if (objectId == null) {
			final MessageDigest md = Constants.newMessageDigest();
			md.update(Constants.encodeASCII(getType()));
			md.update((byte) ' ');
			md.update(Constants.encodeASCII(getSize()));
			md.update((byte) 0);
			md.update(getBytes());
			objectId = new ObjectId(md.digest());
		}
		return objectId;
	}

	protected void setId(final ObjectId id) {
		if (objectId != null)
			throw new IllegalStateException("Id already set.");
		objectId = id;
	}

	public abstract String getType() throws IOException;

	public abstract long getSize() throws IOException;

	public abstract byte[] getBytes() throws IOException;
}
