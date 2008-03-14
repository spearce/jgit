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

import org.spearce.jgit.lib.ObjectId;

/** Base object type accessed during revision walking. */
public abstract class RevObject {
	static final int PARSED = 1;

	final ObjectId id;

	int flags;

	RevObject(final ObjectId name) {
		id = name;
	}

	/**
	 * Get the name of this object.
	 * 
	 * @return unique hash of this object.
	 */
	public ObjectId getId() {
		return id;
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
	 * 
	 * @param flag
	 *            the flag to mark on this object, for later testing.
	 */
	public void add(final RevFlag flag) {
		flags |= flag.mask;
	}

	/** Release as much memory as possible from this object. */
	public void dispose() {
		// Nothing needs to be done for most objects.
	}
}
