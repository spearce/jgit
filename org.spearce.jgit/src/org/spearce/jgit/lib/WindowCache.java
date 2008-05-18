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

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.util.zip.Inflater;

/**
 * The WindowCache manages reusable <code>Windows</code> and inflaters used by
 * the other windowed file access classes.
 */
public class WindowCache {
	private static final int KB = 1024;

	private static final int MB = 1024 * KB;

	private static final int bits(int newSize) {
		if (newSize < 4096)
			throw new IllegalArgumentException("Invalid window size");
		if (Integer.bitCount(newSize) != 1)
			throw new IllegalArgumentException("Window size must be power of 2");
		return Integer.numberOfTrailingZeros(newSize);
	}

	private static final Inflater[] inflaterCache;

	private static final int maxByteCount;

	static final int sz;

	static final int szb;

	static final int szm;

	static final boolean mmap;

	static final ReferenceQueue<?> clearedWindowQueue;

	private static final ByteWindow[] windows;

	private static int openWindowCount;

	private static int openByteCount;

	private static int openInflaterCount;

	private static int accessClock;

	static {
		maxByteCount = 10 * MB;
		szb = bits(8 * KB);
		sz = 1 << szb;
		szm = (1 << szb) - 1;
		mmap = false;
		windows = new ByteWindow[maxByteCount / sz];
		inflaterCache = new Inflater[4];
		clearedWindowQueue = new ReferenceQueue<Object>();
	}

	/**
	 * Modify the configuration of the window cache.
	 * <p>
	 * The new configuration is applied immediately. If the new limits are
	 * smaller than what what is currently cached, older entries will be purged
	 * as soon as possible to allow the cache to meet the new limit.
	 * 
	 * @param packedGitLimit
	 *            maximum number of bytes to hold within this instance.
	 * @param packedGitWindowSize
	 *            number of bytes per window within the cache.
	 * @param packedGitMMAP
	 *            true to enable use of mmap when creating windows.
	 * @param deltaBaseCacheLimit
	 *            number of bytes to hold in the delta base cache.
	 */
	public static void reconfigure(final int packedGitLimit,
			final int packedGitWindowSize, final boolean packedGitMMAP,
			final int deltaBaseCacheLimit) {
		// fix me
		UnpackedObjectCache.reconfigure(deltaBaseCacheLimit);
	}

	synchronized static Inflater borrowInflater() {
		if (openInflaterCount > 0) {
			final Inflater r = inflaterCache[--openInflaterCount];
			inflaterCache[openInflaterCount] = null;
			return r;
		}
		return new Inflater(false);
	}

	synchronized static void returnInflater(final Inflater i) {
		if (openInflaterCount == inflaterCache.length)
			i.end();
		else
			inflaterCache[openInflaterCount++] = i;
	}

	/**
	 * Get a specific window.
	 * 
	 * @param curs
	 *            an active cursor object to maintain the window reference while
	 *            the caller needs it.
	 * @param wp
	 *            the provider of the window. If the window is not currently in
	 *            the cache then the provider will be asked to load it.
	 * @param id
	 *            the id, unique only within the scope of the specific provider
	 *            <code>wp</code>. Typically this id is the byte offset
	 *            within the file divided by the window size, but its meaning is
	 *            left open to the provider.
	 * @throws IOException
	 *             the window was not found in the cache and the given provider
	 *             was unable to load the window on demand.
	 */
	public static synchronized final void get(final WindowCursor curs,
			final WindowedFile wp, final int id) throws IOException {
		int idx = binarySearch(wp, id);
		if (0 <= idx) {
			final ByteWindow<?> w = windows[idx];
			if ((curs.handle = w.get()) != null) {
				w.lastAccessed = ++accessClock;
				curs.window = w;
				return;
			}
		}

		if (++wp.openCount == 1) {
			try {
				wp.cacheOpen();
			} catch (IOException ioe) {
				wp.openCount = 0;
				throw ioe;
			} catch (RuntimeException ioe) {
				wp.openCount = 0;
				throw ioe;
			} catch (Error ioe) {
				wp.openCount = 0;
				throw ioe;
			}

			// The cacheOpen may have mapped the window we are trying to
			// map ourselves. Retrying the search ensures that does not
			// happen to us.
			//
			idx = binarySearch(wp, id);
			if (0 <= idx) {
				final ByteWindow<?> w = windows[idx];
				if ((curs.handle = w.get()) != null) {
					w.lastAccessed = ++accessClock;
					curs.window = w;
					return;
				}
			}
		}

		idx = -(idx + 1);
		for (;;) {
			final ByteWindow<?> w = (ByteWindow<?>) clearedWindowQueue.poll();
			if (w == null)
				break;
			final int oldest = binarySearch(w.provider, w.id);
			if (oldest < 0 || windows[oldest] != w)
				continue; // Must have been evicted by our other controls.

			final WindowedFile p = w.provider;
			if (--p.openCount == 0 && p != wp)
				p.cacheClose();

			openByteCount -= w.size;
			final int toMove = openWindowCount - oldest - 1;
			if (toMove > 0)
				System.arraycopy(windows, oldest + 1, windows, oldest, toMove);
			windows[--openWindowCount] = null;
			if (oldest < idx)
				idx--;
		}

		final int wSz = wp.getWindowSize(id);
		while (openWindowCount == windows.length
				|| (openWindowCount > 0 && openByteCount + wSz > maxByteCount)) {
			int oldest = 0;
			for (int k = openWindowCount - 1; k > 0; k--) {
				if (windows[k].lastAccessed < windows[oldest].lastAccessed)
					oldest = k;
			}

			final ByteWindow w = windows[oldest];
			final WindowedFile p = w.provider;
			if (--p.openCount == 0 && p != wp)
				p.cacheClose();

			openByteCount -= w.size;
			final int toMove = openWindowCount - oldest - 1;
			if (toMove > 0)
				System.arraycopy(windows, oldest + 1, windows, oldest, toMove);
			windows[--openWindowCount] = null;
			w.enqueue();
			if (oldest < idx)
				idx--;
		}

		if (idx < 0)
			idx = 0;
		final int toMove = openWindowCount - idx;
		if (toMove > 0)
			System.arraycopy(windows, idx, windows, idx + 1, toMove);
		wp.loadWindow(curs, id);
		windows[idx] = curs.window;
		openWindowCount++;
		openByteCount += curs.window.size;
	}

	private static final int binarySearch(final WindowedFile sprov,
			final int sid) {
		if (openWindowCount == 0)
			return -1;
		final int shc = sprov.hash;
		int high = openWindowCount;
		int low = 0;
		do {
			final int mid = (low + high) / 2;
			final ByteWindow mw = windows[mid];
			if (mw.provider == sprov && mw.id == sid)
				return mid;
			final int mhc = mw.provider.hash;
			if (mhc < shc || (shc == mhc && mw.id < sid))
				low = mid + 1;
			else
				high = mid;
		} while (low < high);
		return -(low + 1);
	}

	/**
	 * Remove all windows associated with a specific provider.
	 * <p>
	 * Providers should invoke this method as part of their cleanup/close
	 * routines, ensuring that the window cache releases all windows that cannot
	 * ever be requested again.
	 * </p>
	 * 
	 * @param wp
	 *            the window provider whose windows should be removed from the
	 *            cache.
	 */
	public static synchronized final void purge(final WindowedFile wp) {
		int d = 0;
		for (int s = 0; s < openWindowCount; s++) {
			final ByteWindow win = windows[s];
			if (win.provider != wp)
				windows[d++] = win;
			else
				openByteCount -= win.size;
		}
		openWindowCount = d;

		if (wp.openCount > 0) {
			wp.openCount = 0;
			wp.cacheClose();
		}
	}

	private WindowCache() {
		throw new UnsupportedOperationException();
	}
}
