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

/**
 * Loads windows on demand from their backing store (like a file).
 */
public abstract class WindowProvider {
    /**
         * Load a specific window.
         * <p>
         * The disk IO does not need to fully complete during this method. For
         * example if the returned buffer is a MappedByteBuffer then the disk IO
         * to load all bytes contained within the window is probably not yet
         * completed, but will be done on demand by the operating system as
         * bytes are copied. On the other hand if the returned buffer is just a
         * byte[] in Java then all bytes have been fully loaded, and this method
         * might take a while.
         * </p>
         * <p>
         * Please take note that this method is invoked while the cache holds a
         * lock on itself. Consequently it shouldn't take too long, otherwise
         * the method might prevent other threads from accessing the cache.
         * </p>
         * 
         * @param id
         *                the id number of this window. See
         *                {@link WindowCache#get(org.spearce.jgit.lib.WindowCache.WindowProvider, int)}
         *                for details.
         * @return a byte buffer for this window's data. Never null. The
         *         returned byffer may be smaller than the estimate supplied by
         *         {@link #getWindowSize(int)}.
         * @throws IOException
         *                 the window could not be loaded due to an operating
         *                 system issue.
         */
    public abstract ByteWindow loadWindow(int id) throws IOException;

    /**
         * Estimate the size of a given window.
         * <p>
         * This estimate is used by the cache to determine when loading the
         * window will push the cache over its maximum limit, forcing it to
         * unload one or more windows.
         * </p>
         * 
         * @param idthe
         *                id number of the window to estimate. See
         *                {@link WindowCache#get(org.spearce.jgit.lib.WindowCache.WindowProvider, int)}
         *                for details.
         * @return total number of bytes in the requested window.
         */
    public abstract int getWindowSize(int id);
}