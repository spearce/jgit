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

import java.util.AbstractList;

/**
 * An ordered list of {@link RevObject} subclasses.
 * 
 * @param <E>
 *            type of subclass of RevObject the list is storing.
 */
public class RevObjectList<E extends RevObject> extends AbstractList<E> {
	static final int BLOCK_SHIFT = 8;

	static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;

	Block contents;

	int size;

	/** Create an empty object list. */
	public RevObjectList() {
		clear();
	}

	public void add(final int index, final E element) {
		if (index != size)
			throw new UnsupportedOperationException("Not add-at-end: " + index);
		set(index, element);
		size++;
	}

	public E set(int index, E element) {
		Block s = contents;
		while (index >> s.shift >= BLOCK_SIZE) {
			s = new Block(s.shift + BLOCK_SHIFT);
			s.contents[0] = contents;
			contents = s;
		}
		while (s.shift > 0) {
			final int i = index >> s.shift;
			index -= i << s.shift;
			if (s.contents[i] == null)
				s.contents[i] = new Block(s.shift - BLOCK_SHIFT);
			s = (Block) s.contents[i];
		}
		final Object old = s.contents[index];
		s.contents[index] = element;
		return (E) old;
	}

	public E get(int index) {
		Block s = contents;
		if (index >> s.shift >= 1024)
			return null;
		while (s != null && s.shift > 0) {
			final int i = index >> s.shift;
			index -= i << s.shift;
			s = (Block) s.contents[i];
		}
		return s != null ? (E) s.contents[index] : null;
	}

	public int size() {
		return size;
	}

	@Override
	public void clear() {
		contents = new Block(0);
		size = 0;
	}

	static class Block {
		final Object[] contents = new Object[BLOCK_SIZE];

		final int shift;

		Block(final int s) {
			shift = s;
		}
	}
}
