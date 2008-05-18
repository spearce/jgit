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

import java.lang.ref.SoftReference;

class UnpackedObjectCache {
	private static final int CACHE_SZ = 256;

	private static final int MB = 1024 * 1024;

	private static final SoftReference<Entry> DEAD;

	private static int hash(final WindowedFile pack, final long position) {
		int h = pack.hash + (int) position;
		h += h >> 16;
		h += h >> 8;
		return h % CACHE_SZ;
	}

	private static int maxByteCount;

	private static final Slot[] cache;

	private static Slot lruHead;

	private static Slot lruTail;

	private static int openByteCount;

	static {
		DEAD = new SoftReference<Entry>(null);
		maxByteCount = 10 * MB;

		cache = new Slot[CACHE_SZ];
		for (int i = 0; i < CACHE_SZ; i++)
			cache[i] = new Slot();
	}

	static synchronized void reconfigure(final int dbLimit) {
		if (maxByteCount != dbLimit) {
			maxByteCount = dbLimit;
			releaseMemory();
		}
	}

	static synchronized Entry get(final WindowedFile pack, final long position) {
		final Slot e = cache[hash(pack, position)];
		if (e.provider == pack && e.position == position) {
			final Entry buf = e.data.get();
			if (buf != null) {
				moveToHead(e);
				return buf;
			}
		}
		return null;
	}

	static synchronized void store(final WindowedFile pack,
			final long position, final byte[] data, final int objectType) {
		if (data.length > maxByteCount)
			return; // Too large to cache.

		final Slot e = cache[hash(pack, position)];
		clearEntry(e);

		openByteCount += data.length;
		releaseMemory();

		e.provider = pack;
		e.position = position;
		e.data = new SoftReference<Entry>(new Entry(data, objectType));
		moveToHead(e);
	}

	private static void releaseMemory() {
		while (openByteCount > maxByteCount && lruTail != null) {
			final Slot currOldest = lruTail;
			final Slot nextOldest = currOldest.lruPrev;

			clearEntry(currOldest);
			currOldest.lruPrev = null;
			currOldest.lruNext = null;

			if (nextOldest == null)
				lruHead = null;
			else
				nextOldest.lruNext = null;
			lruTail = nextOldest;
		}
	}

	static synchronized void purge(final WindowedFile file) {
		for (final Slot e : cache) {
			if (e.provider == file) {
				clearEntry(e);
				unlink(e);
			}
		}
	}

	private static void moveToHead(final Slot e) {
		unlink(e);
		e.lruPrev = null;
		e.lruNext = lruHead;
		if (lruHead != null)
			lruHead.lruPrev = e;
		else
			lruTail = e;
		lruHead = e;
	}

	private static void unlink(final Slot e) {
		final Slot prev = e.lruPrev;
		final Slot next = e.lruNext;
		if (prev != null)
			prev.lruNext = next;
		if (next != null)
			next.lruPrev = prev;
	}

	private static void clearEntry(final Slot e) {
		final Entry old = e.data.get();
		if (old != null)
			openByteCount -= old.data.length;
		e.provider = null;
		e.data = DEAD;
	}

	private UnpackedObjectCache() {
		throw new UnsupportedOperationException();
	}

	static class Entry {
		final byte[] data;

		final int type;

		Entry(final byte[] aData, final int aType) {
			data = aData;
			type = aType;
		}
	}

	private static class Slot {
		Slot lruPrev;

		Slot lruNext;

		WindowedFile provider;

		long position;

		SoftReference<Entry> data = DEAD;
	}
}
