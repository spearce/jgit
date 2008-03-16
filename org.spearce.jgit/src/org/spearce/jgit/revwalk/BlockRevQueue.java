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

abstract class BlockRevQueue {
	protected BlockFreeList free;

	/** Create an empty revision queue. */
	protected BlockRevQueue() {
		free = new BlockFreeList();
	}

	/**
	 * Insert the commit pointer at the end of the queue.
	 * 
	 * @param c
	 *            the commit to insert into the queue.
	 */
	public abstract void add(final RevCommit c);

	/**
	 * Remove the first commit from the queue.
	 * 
	 * @return the first commit of this queue.
	 */
	public abstract RevCommit pop();

	/** Remove all entries from this queue. */
	public abstract void clear();

	/**
	 * Reconfigure this queue to share the same free list as another.
	 * <p>
	 * Multiple revision queues can be connected to the same free list, making
	 * it less expensive for applications to shuttle commits between them. This
	 * method arranges for the receiver to take from / return to the same free
	 * list as the supplied queue.
	 * <p>
	 * Free lists are not thread-safe. Applications must ensure that all queues
	 * sharing the same free list are doing so from only a single thread.
	 * 
	 * @param q
	 *            the other queue we will steal entries from.
	 */
	public void shareFreeList(final BlockRevQueue q) {
		free = q.free;
	}

	static final class BlockFreeList {
		private Block next;

		Block newBlock() {
			Block b = next;
			if (b == null)
				return new Block();
			next = b.next;
			b.clear();
			return b;
		}

		void freeBlock(final Block b) {
			b.next = next;
			next = b;
		}

		void clear() {
			next = null;
		}
	}

	static final class Block {
		private static final int BLOCK_SIZE = 256;

		/** Next block in our chain of blocks; null if we are the last. */
		Block next;

		/** Our table of queued commits. */
		final RevCommit[] commits = new RevCommit[BLOCK_SIZE];

		/** Next valid entry in {@link #commits}. */
		int headIndex;

		/** Next free entry in {@link #commits} for addition at. */
		int tailIndex;

		boolean isFull() {
			return tailIndex == BLOCK_SIZE;
		}

		boolean isEmpty() {
			return headIndex == tailIndex;
		}

		boolean canUnpop() {
			return headIndex > 0;
		}

		void add(final RevCommit c) {
			commits[tailIndex++] = c;
		}

		void unpop(final RevCommit c) {
			commits[--headIndex] = c;
		}

		RevCommit pop() {
			return commits[headIndex++];
		}

		RevCommit peek() {
			return commits[headIndex];
		}

		void clear() {
			next = null;
			headIndex = 0;
			tailIndex = 0;
		}

		void resetToMiddle() {
			headIndex = tailIndex = BLOCK_SIZE / 2;
		}

		void resetToEnd() {
			headIndex = tailIndex = BLOCK_SIZE;
		}
	}
}
