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

/** A queue of commits sorted by commit time order. */
public class DateRevQueue {
	private Entry head;

	/**
	 * Insert the commit pointer by commit time ordering.
	 * 
	 * @param c
	 *            the commit to insert into the queue.
	 */
	public void add(final RevCommit c) {
		Entry q = head;
		final long when = c.commitTime;
		final Entry n = new Entry(c);
		if (q == null || when > q.commit.commitTime) {
			n.next = q;
			head = n;
		} else {
			Entry p = q.next;
			while (p != null && p.commit.commitTime > when) {
				q = p;
				p = q.next;
			}
			n.next = q.next;
			q.next = n;
		}
	}

	/**
	 * Remove the newest commit from the queue.
	 * 
	 * @return the next commit with the highest (most recent) time; null if
	 *         there are no more commits available in the queue.
	 */
	public RevCommit pop() {
		final Entry q = head;
		if (q == null)
			return null;
		head = q.next;
		return q.commit;
	}

	/**
	 * Peek at the next commit, without removing it.
	 * 
	 * @return the next available commit; null if there are no commits left.
	 */
	public RevCommit peek() {
		return head != null ? head.commit : null;
	}

	/** Remove all entries from this queue. */
	public void clear() {
		head = null;
	}

	boolean everbodyHasFlag(final int f) {
		for (Entry q = head; q != null; q = q.next) {
			if ((q.commit.flags & f) == 0)
				return false;
		}
		return true;
	}

	public String toString() {
		final StringBuffer s = new StringBuffer();
		for (Entry q = head; q != null; q = q.next) {
			s.append(q.commit.id);
			s.append(' ');
			s.append(q.commit.commitTime);
			s.append('\n');
		}
		return s.toString();
	}

	static class Entry {
		Entry next;

		final RevCommit commit;

		Entry(final RevCommit c) {
			commit = c;
		}
	}
}
