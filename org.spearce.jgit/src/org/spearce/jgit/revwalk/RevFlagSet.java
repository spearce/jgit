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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Multiple application level mark bits for {@link RevObject}s.
 * 
 * @see RevFlag
 */
public class RevFlagSet extends AbstractSet<RevFlag> {
	int mask;

	private final List<RevFlag> active;

	/** Create an empty set of flags. */
	public RevFlagSet() {
		active = new ArrayList<RevFlag>();
	}

	/**
	 * Create a set of flags, copied from an existing set.
	 * 
	 * @param s
	 *            the set to copy flags from.
	 */
	public RevFlagSet(final RevFlagSet s) {
		mask = s.mask;
		active = new ArrayList<RevFlag>(s.active);
	}

	/**
	 * Create a set of flags, copied from an existing collection.
	 * 
	 * @param s
	 *            the collection to copy flags from.
	 */
	public RevFlagSet(final Collection<RevFlag> s) {
		this();
		addAll(s);
	}

	@Override
	public boolean contains(final Object o) {
		if (o instanceof RevFlag)
			return (mask & ((RevFlag) o).mask) != 0;
		return false;
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		if (c instanceof RevFlagSet) {
			final int cMask = ((RevFlagSet) c).mask;
			return (mask & cMask) == cMask;
		}
		return super.containsAll(c);
	}

	@Override
	public boolean add(final RevFlag flag) {
		if ((mask & flag.mask) != 0)
			return false;
		mask |= flag.mask;
		int p = 0;
		while (p < active.size() && active.get(p).mask < flag.mask)
			p++;
		active.add(p, flag);
		return true;
	}

	@Override
	public boolean remove(final Object o) {
		final RevFlag flag = (RevFlag) o;
		if ((mask & flag.mask) == 0)
			return false;
		mask &= ~flag.mask;
		for (int i = 0; i < active.size(); i++)
			if (active.get(i).mask == flag.mask)
				active.remove(i);
		return true;
	}

	@Override
	public Iterator<RevFlag> iterator() {
		final Iterator<RevFlag> i = active.iterator();
		return new Iterator<RevFlag>() {
			private RevFlag current;

			public boolean hasNext() {
				return i.hasNext();
			}

			public RevFlag next() {
				return current = i.next();
			}

			public void remove() {
				mask &= ~current.mask;
				i.remove();
			}
		};
	}

	@Override
	public int size() {
		return active.size();
	}
}
