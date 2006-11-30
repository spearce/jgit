/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.zip.Inflater;

public class WindowCache {
    private final Inflater[] inflaterCache;

    private final int maxByteCount;

    private final ByteWindow[] windows;

    private int openWindowCount;

    private int openByteCount;

    private int openInflaterCount;

    private int accessClock;

    /**
         * Create a new window cache.
         * 
         * @param maxBytes
         *                maximum number of bytes to have in the cache at one
         *                time. If loading another window will cause the cache
         *                to exceed this limit then less-recently used windows
         *                will be released for garbage collection before the new
         *                window is loaded.
         * @param maxOpen
         *                maximum number of windows to have open in the cache at
         *                one time. If loading another window will cause the
         *                cache to exceed this limit on open windows then a
         *                less-recently used window will be released for garbage
         *                collection before the new window is loaded.
         */
    public WindowCache(final int maxBytes, final int maxOpen) {
	maxByteCount = maxBytes;
	windows = new ByteWindow[maxOpen];
	inflaterCache = new Inflater[maxOpen];
    }

    synchronized Inflater borrowInflater() {
	if (openInflaterCount > 0) {
	    final Inflater r = inflaterCache[--openInflaterCount];
	    inflaterCache[openInflaterCount] = null;
	    return r;
	}
	return new Inflater(false);
    }

    synchronized void returnInflater(final Inflater i) {
	if (openInflaterCount == inflaterCache.length)
	    i.end();
	else
	    inflaterCache[openInflaterCount++] = i;
    }

    /**
         * Get a specific window.
         * 
         * @param wp
         *                the provider of the window. If the window is not
         *                currently in the cache then the provider will be asked
         *                to load it.
         * @param id
         *                the id, unique only within the scope of the specific
         *                provider <code>wp</code>. Typically this id is the
         *                byte offset within the file divided by the window
         *                size, but its meaning is left open to the provider.
         * @return the requested window. Never null.
         * @throws IOException
         *                 the window was not found in the cache and the given
         *                 provider was unable to load the window on demand.
         */
    public synchronized final ByteWindow get(final WindowProvider wp,
	    final int id) throws IOException {
	int idx = binarySearch(wp, id);
	if (0 <= idx) {
	    final ByteWindow w = windows[idx];
	    w.lastAccessed = ++accessClock;
	    return w;
	}

	final int wSz = wp.getWindowSize(id);
	while (openWindowCount == windows.length
		|| (openWindowCount > 0 && openByteCount + wSz > maxByteCount)) {
	    int oldest = 0;
	    for (int k = openWindowCount - 1; k > 0; k--) {
		if (windows[k].lastAccessed < windows[oldest].lastAccessed)
		    oldest = k;
	    }

	    openByteCount -= windows[oldest].size();
	    final int toMove = openWindowCount - oldest - 1;
	    if (toMove > 0)
		System.arraycopy(windows, oldest + 1, windows, oldest, toMove);
	    windows[--openWindowCount] = null;
	}

	idx = -(idx + 1);
	final int toMove = openWindowCount - idx;
	if (toMove > 0)
	    System.arraycopy(windows, idx, windows, idx + 1, toMove);
	final ByteWindow w = wp.loadWindow(id);
	windows[idx] = w;
	openWindowCount++;
	openByteCount += w.size();
	return w;
    }

    private final int binarySearch(final WindowProvider sprov, final int sid) {
	if (openWindowCount == 0)
	    return -1;
	final int shc = System.identityHashCode(sprov);
	int high = openWindowCount;
	int low = 0;
	do {
	    final int mid = (low + high) / 2;
	    final ByteWindow mw = windows[mid];
	    if (mw.provider == sprov && mw.id == sid)
		return mid;
	    final int mhc = System.identityHashCode(mw.provider);
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
         * routines, ensuring that the window cache releases all windows that
         * cannot ever be requested again.
         * </p>
         * 
         * @param wp
         *                the window provider whose windows should be removed
         *                from the cache.
         */
    public synchronized final void purge(final WindowProvider wp) {
	int d = 0;
	for (int s = 0; s < openWindowCount; s++) {
	    final ByteWindow win = windows[s];
	    if (win.provider != wp)
		windows[d++] = win;
	    else
		openByteCount -= win.size();
	}
	openWindowCount = d;
    }
}
