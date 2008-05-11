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

/**
 * Base class for a set of loaders for different representations of Git objects.
 * New loaders are constructed for every object.
 */
public abstract class ObjectLoader {
	private ObjectId objectId;

	/**
	 * @return the id of this object, possibly computed on demand
	 * @throws IOException
	 */
	public ObjectId getId() throws IOException {
		if (objectId == null) {
			final MessageDigest md = Constants.newMessageDigest();
			md.update(Constants.encodeASCII(Constants.typeString(getType())));
			md.update((byte) ' ');
			md.update(Constants.encodeASCII(getSize()));
			md.update((byte) 0);
			md.update(getCachedBytes());
			objectId = ObjectId.fromRaw(md.digest());
		}
		return objectId;
	}

	/**
	 * Set the SHA-1 id of the object handled by this loader
	 * 
	 * @param id
	 */
	protected void setId(final ObjectId id) {
		if (objectId != null)
			throw new IllegalStateException("Id already set.");
		objectId = id;
	}

	/**
	 * @return Git in pack object type, see {@link Constants}.
	 * @throws IOException
	 */
	public abstract int getType() throws IOException;

	/**
	 * @return size of object in bytes
	 * @throws IOException
	 */
	public abstract long getSize() throws IOException;

	/**
	 * Obtain a copy of the bytes of this object.
	 * <p>
	 * Unlike {@link #getCachedBytes()} this method returns an array that might
	 * be modified by the caller.
	 * 
	 * @return the bytes of this object.
	 * @throws IOException
	 *             the object cannot be read.
	 */
	public abstract byte[] getBytes() throws IOException;

	/**
	 * Obtain a reference to the (possibly cached) bytes of this object.
	 * <p>
	 * This method offers direct access to the internal caches, potentially
	 * saving on data copies between the internal cache and higher level code.
	 * Callers who receive this reference <b>must not</b> modify its contents.
	 * Changes (if made) will affect the cache but not the repository itself.
	 * 
	 * @return the cached bytes of this object. Do not modify it.
	 * @throws IOException
	 *             the object cannot be read.
	 */
	public abstract byte[] getCachedBytes() throws IOException;
}
