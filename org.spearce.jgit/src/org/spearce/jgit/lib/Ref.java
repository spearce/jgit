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
 * Pairing of a name and the {@link ObjectId} it currently has.
 * <p>
 * A ref in Git is (more or less) a variable that holds a single object
 * identifier. The object identifier can be any valid Git object (blob, tree,
 * commit, annotated tag, ...).
 */
public class Ref {
	private final String name;

	private ObjectId objectId;

	/**
	 * Create a new ref pairing.
	 * 
	 * @param refName
	 *            name of this ref.
	 * @param id
	 *            current value of the ref. May be null to indicate a ref that
	 *            does not exist yet.
	 */
	public Ref(final String refName, final ObjectId id) {
		name = refName;
		objectId = id;
	}

	/**
	 * What this ref is called within the repository.
	 * 
	 * @return name of this ref.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Cached value of this ref.
	 * 
	 * @return the value of this ref at the last time we read it.
	 */
	public ObjectId getObjectId() {
		return objectId;
	}

	public String toString() {
		return "Ref[" + name + "=" + getObjectId() + "]";
	}
}
