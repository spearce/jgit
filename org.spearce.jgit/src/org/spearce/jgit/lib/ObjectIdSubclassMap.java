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
package org.spearce.jgit.lib;

/**
 * Fast, efficient map specifically for {@link ObjectId} subclasses.
 * <p>
 * This map provides an efficient translation from any ObjectId instance to a
 * cached subclass of ObjectId that has the same value.
 * <p>
 * Raw value equality is tested when comparing two ObjectIds (or subclasses),
 * not reference equality and not <code>.equals(Object)</code> equality. This
 * allows subclasses to override <code>equals</code> to supply their own
 * extended semantics.
 * 
 * @param <V>
 *            type of subclass of ObjectId that will be stored in the map.
 */
public class ObjectIdSubclassMap<V extends ObjectId> {
	private int size;

	private V[] obj_hash;

	/** Create an empty map. */
	public ObjectIdSubclassMap() {
		obj_hash = createArray(32);
	}

	/**
	 * Lookup an existing mapping.
	 * 
	 * @param toFind
	 *            the object identifier to find.
	 * @return the instance mapped to toFind, or null if no mapping exists.
	 */
	public V get(final ObjectId toFind) {
		int i = index(toFind);
		V obj;

		while ((obj = obj_hash[i]) != null) {
			if (ObjectId.equals(obj, toFind))
				return obj;
			if (++i == obj_hash.length)
				i = 0;
		}
		return null;
	}

	/**
	 * Store an object for future lookup.
	 * <p>
	 * An existing mapping for <b>must not</b> be in this map. Callers must
	 * first call {@link #get(ObjectId)} to verify there is no current mapping
	 * prior to adding a new mapping.
	 * 
	 * @param newValue
	 *            the object to store.
	 * @param
	 *            <Q>
	 *            type of instance to store.
	 */
	public <Q extends V> void add(final Q newValue) {
		if (obj_hash.length - 1 <= size * 2)
			grow();
		insert(newValue);
		size++;
	}

	private final int index(final ObjectId id) {
		return (id.w1 >>> 1) % obj_hash.length;
	}

	private void insert(final V newValue) {
		int j = index(newValue);
		while (obj_hash[j] != null) {
			if (++j >= obj_hash.length)
				j = 0;
		}
		obj_hash[j] = newValue;
	}

	private void grow() {
		final V[] old_hash = obj_hash;
		final int old_hash_size = obj_hash.length;

		obj_hash = createArray(2 * old_hash_size);
		for (int i = 0; i < old_hash_size; i++) {
			final V obj = old_hash[i];
			if (obj != null)
				insert(obj);
		}
	}

	@SuppressWarnings("unchecked")
	private final V[] createArray(final int sz) {
		return (V[]) new ObjectId[sz];
	}
}
