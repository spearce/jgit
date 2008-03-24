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

import java.io.IOException;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;

/**
 * An ordered list of {@link RevCommit} subclasses.
 * 
 * @param <E>
 *            type of subclass of RevCommit the list is storing.
 */
public class RevCommitList<E extends RevCommit> extends RevObjectList<E> {
	private RevWalk walker;

	@Override
	public void clear() {
		super.clear();
		walker = null;
	}

	/**
	 * Set the revision walker this list populates itself from.
	 * 
	 * @param w
	 *            the walker to populate from.
	 * @see #fillTo(int)
	 */
	public void source(final RevWalk w) {
		walker = w;
	}

	/**
	 * Is this list still pending more items?
	 * 
	 * @return true if {@link #fillTo(int)} might be able to extend the list
	 *         size when called.
	 */
	public boolean isPending() {
		return walker != null;
	}

	/**
	 * Ensure this list contains at least a specified number of commits.
	 * <p>
	 * The revision walker specified by {@link #source(RevWalk)} is pumped until
	 * the given number of commits are contained in this list. If there are
	 * fewer total commits available from the walk then the method will return
	 * early. Callers can test the final size of the list by {@link #size()} to
	 * determine if the high water mark specified was met.
	 * 
	 * @param highMark
	 *            number of commits the caller wants this list to contain when
	 *            the fill operation is complete.
	 * @throws IOException
	 *             see {@link RevWalk#next()}
	 * @throws IncorrectObjectTypeException
	 *             see {@link RevWalk#next()}
	 * @throws MissingObjectException
	 *             see {@link RevWalk#next()}
	 */
	public void fillTo(final int highMark) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if (walker == null || size > highMark)
			return;

		Generator p = walker.pending;
		RevCommit c = p.next();
		if (c == null) {
			walker.pending = EndGenerator.INSTANCE;
			walker = null;
			return;
		}
		enter(size, (E) c);
		add((E) c);
		p = walker.pending;

		while (size <= highMark) {
			int index = size;
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

			final Object[] dst = s.contents;
			while (size <= highMark && index < BLOCK_SIZE) {
				c = p.next();
				if (c == null) {
					walker.pending = EndGenerator.INSTANCE;
					walker = null;
					return;
				}
				enter(size++, (E) c);
				dst[index++] = c;
			}

			filledBatch();
		}
	}

	/**
	 * Optional callback invoked when commits enter the list by fillTo.
	 * <p>
	 * This method is only called during {@link #fillTo(int)}.
	 * 
	 * @param index
	 *            the list position this object will appear at.
	 * @param e
	 *            the object being added (or set) into the list.
	 */
	protected void enter(final int index, final E e) {
		// Do nothing by default.
	}

	/**
	 * Optional callback invoked per batch of commits added.
	 * <p>
	 * This method is only called during {@link #fillTo(int)}, and is invoked
	 * once per every block of commits added. Applications might wish to
	 * override this method to trigger a UI refresh once sufficient data is
	 * loaded into the list.
	 */
	protected void filledBatch() {
		// Do nothing by default.
	}
}
