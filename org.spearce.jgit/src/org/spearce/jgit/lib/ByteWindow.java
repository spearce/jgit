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

/**
 * A window of data currently stored within a cache.
 * <p>
 * All bytes in the window can be assumed to be "immediately available", that is
 * they are very likely already in memory, unless the operating system's memory
 * is very low and has paged part of this process out to disk. Therefore copying
 * bytes from a window is very inexpensive.
 * </p>
 */
public abstract class ByteWindow {
    final WindowProvider provider;

    final int id;

    int lastAccessed;

    protected ByteWindow(final WindowProvider o, final int d) {
	provider = o;
	id = d;
    }

    /**
         * Copy bytes from the window to a caller supplied buffer.
         * 
         * @param pos
         *                offset within the window to start copying from.
         * @param dstbuf
         *                destination buffer to copy into.
         * @param dstoff
         *                offset within <code>dstbuf</code> to start copying
         *                into.
         * @param cnt
         *                number of bytes to copy. This value may exceed the
         *                number of bytes remaining in the window starting at
         *                offset <code>pos</code>.
         * @return number of bytes actually copied; this may be less than
         *         <code>cnt</code> if <code>cnt</code> exceeded the number
         *         of bytes available.
         */
    public abstract int copy(int pos, byte[] dstbuf, int dstoff, int cnt);

    /**
         * Get the total size of this window.
         * 
         * @return number of bytes in this window.
         */
    public abstract int size();
}