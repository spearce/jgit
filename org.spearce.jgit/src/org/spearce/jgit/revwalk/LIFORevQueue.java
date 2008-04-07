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

/** A queue of commits in LIFO order. */
public class LIFORevQueue extends BlockRevQueue {
	private Block head;

	/** Create an empty LIFO queue. */
	public LIFORevQueue() {
		super();
	}

	LIFORevQueue(final Generator s) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		super(s);
	}

	public void add(final RevCommit c) {
		Block b = head;
		if (b == null || !b.canUnpop()) {
			b = free.newBlock();
			b.resetToEnd();
			b.next = head;
			head = b;
		}
		b.unpop(c);
	}

	public RevCommit next() {
		final Block b = head;
		if (b == null)
			return null;

		final RevCommit c = b.pop();
		if (b.isEmpty()) {
			head = b.next;
			free.freeBlock(b);
		}
		return c;
	}

	public void clear() {
		head = null;
		free.clear();
	}

	boolean everbodyHasFlag(final int f) {
		for (Block b = head; b != null; b = b.next) {
			for (int i = b.headIndex; i < b.tailIndex; i++)
				if ((b.commits[i].flags & f) == 0)
					return false;
		}
		return true;
	}

	boolean anybodyHasFlag(final int f) {
		for (Block b = head; b != null; b = b.next) {
			for (int i = b.headIndex; i < b.tailIndex; i++)
				if ((b.commits[i].flags & f) != 0)
					return true;
		}
		return false;
	}

	public String toString() {
		final StringBuffer s = new StringBuffer();
		for (Block q = head; q != null; q = q.next) {
			for (int i = q.headIndex; i < q.tailIndex; i++) {
				s.append(q.commits[i]);
				s.append(' ');
				s.append(q.commits[i].commitTime);
				s.append('\n');
			}
		}
		return s.toString();
	}
}
