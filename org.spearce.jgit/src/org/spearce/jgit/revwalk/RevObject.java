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
package org.spearce.jgit.revwalk;

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.ObjectId;

/** Base object type accessed during revision walking. */
public abstract class RevObject extends ObjectId {
	static final int PARSED = 1;

	int flags;

	RevObject(final AnyObjectId name) {
		super(name);
	}

	/**
	 * Get the name of this object.
	 * 
	 * @return unique hash of this object.
	 */
	public ObjectId getId() {
		return this;
	}

	@Override
	public boolean equals(final ObjectId o) {
		return this == o;
	}

	@Override
	public boolean equals(final Object o) {
		return this == o;
	}

	/**
	 * Test to see if the flag has been set on this object.
	 * 
	 * @param flag
	 *            the flag to test.
	 * @return true if the flag has been added to this object; false if not.
	 */
	public boolean has(final RevFlag flag) {
		return (flags & flag.mask) != 0;
	}

	/**
	 * Add a flag to this object.
	 * <p>
	 * If the flag is already set on this object then the method has no effect.
	 * 
	 * @param flag
	 *            the flag to mark on this object, for later testing.
	 */
	public void add(final RevFlag flag) {
		flags |= flag.mask;
	}

	/**
	 * Remove a flag from this object.
	 * <p>
	 * If the flag is not set on this object then the method has no effect.
	 * 
	 * @param flag
	 *            the flag to remove from this object.
	 */
	public void remove(final RevFlag flag) {
		flags &= ~flag.mask;
	}

	/** Release as much memory as possible from this object. */
	public void dispose() {
		// Nothing needs to be done for most objects.
	}
}
