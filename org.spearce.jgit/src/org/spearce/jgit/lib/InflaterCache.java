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

import java.util.zip.Inflater;

/** Creates zlib based inflaters as necessary for object decompression. */
public class InflaterCache {
	private static final int SZ = 4;

	private static final Inflater[] inflaterCache;

	private static int openInflaterCount;

	static {
		inflaterCache = new Inflater[SZ];
	}

	/**
	 * Obtain an Inflater for decompression.
	 * <p>
	 * Inflaters obtained through this cache should be returned (if possible) by
	 * {@link #release(Inflater)} to avoid garbage collection and reallocation.
	 * 
	 * @return an available inflater. Never null.
	 */
	public synchronized static Inflater get() {
		if (openInflaterCount > 0) {
			final Inflater r = inflaterCache[--openInflaterCount];
			inflaterCache[openInflaterCount] = null;
			return r;
		}
		return new Inflater(false);
	}

	/**
	 * Release an inflater previously obtained from this cache.
	 * 
	 * @param i
	 *            the inflater to return. May be null, in which case this method
	 *            does nothing.
	 */
	public static void release(final Inflater i) {
		if (i == null)
			return;

		if (openInflaterCount == SZ) {
			i.end();
			return;
		}

		i.reset();
		releaseImpl(i);
	}

	private static synchronized void releaseImpl(final Inflater i) {
		if (openInflaterCount == SZ)
			i.end();
		else
			inflaterCache[openInflaterCount++] = i;
	}

	private InflaterCache() {
		throw new UnsupportedOperationException();
	}
}
