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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A window of data currently stored within a cache.
 * <p>
 * All bytes in the window can be assumed to be "immediately available", that is
 * they are very likely already in memory, unless the operating system's memory
 * is very low and has paged part of this process out to disk. Therefore copying
 * bytes from a window is very inexpensive.
 * </p>
 * 
 * @param <T>
 *            type of object reference used to manage the window data.
 */
abstract class ByteWindow<T> extends SoftReference<T> {
	final WindowedFile provider;

	final int id;

	final int size;

	int lastAccessed;

	final long start;

	final long end;

	/**
	 * Constructor for ByteWindow.
	 * 
	 * @param o
	 *            the WindowedFile providing data access
	 * @param pos
	 *            the position in the file the data comes from.
	 * @param d
	 *            an id provided by the WindowedFile. See
	 *            {@link WindowCache#get(WindowCursor, WindowedFile, long)}.
	 * @param ref
	 *            the object value required to perform data access.
	 * @param sz
	 *            the total number of bytes in this window.
	 */
	@SuppressWarnings("unchecked")
	ByteWindow(final WindowedFile o, final long pos, final int d, final T ref,
			final int sz) {
		super(ref, (ReferenceQueue<T>) WindowCache.clearedWindowQueue);
		provider = o;
		size = sz;
		id = d;
		start = pos;
		end = start + size;
	}

	final boolean contains(final WindowedFile neededFile, final long neededPos) {
		return provider == neededFile && start <= neededPos && neededPos < end;
	}

	/**
	 * Copy bytes from the window to a caller supplied buffer.
	 * 
	 * @param ref
	 *            the object value required to perform data access.
	 * @param pos
	 *            offset within the file to start copying from.
	 * @param dstbuf
	 *            destination buffer to copy into.
	 * @param dstoff
	 *            offset within <code>dstbuf</code> to start copying into.
	 * @param cnt
	 *            number of bytes to copy. This value may exceed the number of
	 *            bytes remaining in the window starting at offset
	 *            <code>pos</code>.
	 * @return number of bytes actually copied; this may be less than
	 *         <code>cnt</code> if <code>cnt</code> exceeded the number of
	 *         bytes available.
	 */
	final int copy(T ref, long pos, byte[] dstbuf, int dstoff, int cnt) {
		return copy(ref, (int) (pos - start), dstbuf, dstoff, cnt);
	}

	/**
	 * Copy bytes from the window to a caller supplied buffer.
	 * 
	 * @param ref
	 *            the object value required to perform data access.
	 * @param pos
	 *            offset within the window to start copying from.
	 * @param dstbuf
	 *            destination buffer to copy into.
	 * @param dstoff
	 *            offset within <code>dstbuf</code> to start copying into.
	 * @param cnt
	 *            number of bytes to copy. This value may exceed the number of
	 *            bytes remaining in the window starting at offset
	 *            <code>pos</code>.
	 * @return number of bytes actually copied; this may be less than
	 *         <code>cnt</code> if <code>cnt</code> exceeded the number of
	 *         bytes available.
	 */
	abstract int copy(T ref, int pos, byte[] dstbuf, int dstoff, int cnt);

	/**
	 * Pump bytes into the supplied inflater as input.
	 * 
	 * @param ref
	 *            the object value required to perform data access.
	 * @param pos
	 *            offset within the window to start supplying input from.
	 * @param dstbuf
	 *            destination buffer the inflater should output decompressed
	 *            data to.
	 * @param dstoff
	 *            current offset within <code>dstbuf</code> to inflate into.
	 * @param inf
	 *            the inflater to feed input to. The caller is responsible for
	 *            initializing the inflater as multiple windows may need to
	 *            supply data to the same inflater to completely decompress
	 *            something.
	 * @return updated <code>dstoff</code> based on the number of bytes
	 *         successfully copied into <code>dstbuf</code> by
	 *         <code>inf</code>. If the inflater is not yet finished then
	 *         another window's data must still be supplied as input to finish
	 *         decompression.
	 * @throws DataFormatException
	 *             the inflater encountered an invalid chunk of data. Data
	 *             stream corruption is likely.
	 */
	final int inflate(T ref, long pos, byte[] dstbuf, int dstoff, Inflater inf)
			throws DataFormatException {
		return inflate(ref, (int) (pos - start), dstbuf, dstoff, inf);
	}

	/**
	 * Pump bytes into the supplied inflater as input.
	 * 
	 * @param ref
	 *            the object value required to perform data access.
	 * @param pos
	 *            offset within the window to start supplying input from.
	 * @param dstbuf
	 *            destination buffer the inflater should output decompressed
	 *            data to.
	 * @param dstoff
	 *            current offset within <code>dstbuf</code> to inflate into.
	 * @param inf
	 *            the inflater to feed input to. The caller is responsible for
	 *            initializing the inflater as multiple windows may need to
	 *            supply data to the same inflater to completely decompress
	 *            something.
	 * @return updated <code>dstoff</code> based on the number of bytes
	 *         successfully copied into <code>dstbuf</code> by
	 *         <code>inf</code>. If the inflater is not yet finished then
	 *         another window's data must still be supplied as input to finish
	 *         decompression.
	 * @throws DataFormatException
	 *             the inflater encountered an invalid chunk of data. Data
	 *             stream corruption is likely.
	 */
	abstract int inflate(T ref, int pos, byte[] dstbuf, int dstoff, Inflater inf)
			throws DataFormatException;
}