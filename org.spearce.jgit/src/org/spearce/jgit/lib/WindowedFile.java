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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

/**
 * Read-only cached file access.
 * <p>
 * Supports reading data from large read-only files by loading and caching
 * regions (windows) of the file in memory. Windows may be loaded using either
 * traditional byte[] or by taking advantage of the operating system's virtual
 * memory manager and mapping the file into the JVM's address space.
 * </p>
 * <p>
 * Using no MapMode is the most portable way to access a file and does not run
 * into problems with garbage collection, but will take longer to access as the
 * entire window must be copied from the operating system into the Java byte[]
 * before any single byte can be accessed.
 * </p>
 * <p>
 * Using a specific MapMode will avoid the complete copy by mmaping in the
 * operating system's file buffers, however this may cause problems if a large
 * number of windows are being heavily accessed as the Java garbage collector
 * may not be able to unmap old windows fast enough to permit new windows to be
 * mapped.
 * </p>
 */
public class WindowedFile {
    private static final int bits(int sz) {
	if (sz < 4096)
	    throw new IllegalArgumentException("Invalid window size");

	int b = 0;
	while (sz > 1) {
	    if ((sz & 1) != 0)
		throw new IllegalArgumentException(
			"Window size must be a power of 2");
	    b++;
	    sz >>= 1;
	}
	return b;
    }

    private final WindowCache cache;

    private final int sz;

    private final int szb;

    private final int szm;

    private final Provider wp;

    private final long length;

    /**
         * Open a file for reading through window caching.
         * 
         * @param winCache
         *                the cache this file will maintain its windows in. All
         *                windows in the same cache will be considered for cache
         *                eviction, so multiple files from the same Git
         *                repository probably should use the same window cache.
         * @param file
         *                the file to open. The file will be opened for reading
         *                only, unless {@link MapMode#READ_WRITE} or
         *                {@link MapMode#PRIVATE} is given.
         * @param windowSz
         *                number of bytes within a window. This value must be a
         *                power of 2 and must be at least 4096, or one system
         *                page, whichever is larger. If <code>mapType</code>
         *                is not null then this value should be large (e.g.
         *                several MiBs) to ammortize the high cost of mapping
         *                the file.
         * @param mapType
         *                indicates the type of mmap to use for the byte
         *                windows. Null means don't use mmap, preferring to
         *                allocate a byte[] in Java and reading the file data
         *                into the array. {@link MapMode#READ_ONLY} will use a
         *                read only mmap, however this requires allocation of a
         *                small temporary object during every read.
         *                {@link MapMode#READ_WRITE} will arrange for the file
         *                to opened in read/write mode, however it does not
         *                require that we allocate a temporary object on every
         *                read request.
         * @throws IOException
         *                 the file could not be opened.
         */
    public WindowedFile(final WindowCache winCache, final File file,
	    final int windowSz, final MapMode mapType) throws IOException {
	cache = winCache;
	sz = windowSz;
	szb = bits(windowSz);
	szm = (1 << szb) - 1;
	wp = new Provider(mapType, file);
	length = wp.fd.length();
    }

    /**
         * Read the bytes into a buffer until it is full.
         * <p>
         * This routine always reads until either the requested number of bytes
         * has been copied or EOF has been reached. Consequently callers do not
         * need to invoke it in a loop to make sure their buffer has been fully
         * populated.
         * </p>
         * 
         * @param position
         *                the starting offset, as measured in bytes from the
         *                beginning of this file, to copy from.
         * @param dstbuf
         *                buffer to copy the bytes into.
         * @return total number of bytes read. Always <code>dstbuf.length</code>
         *         unless the requested range to copy is over the end of the
         *         file.
         * @throws IOException
         *                 a necessary window was not found in the window cache
         *                 and trying to load it in from the operating system
         *                 failed.
         */
    public int read(final long position, final byte[] dstbuf)
	    throws IOException {
	return read(position, dstbuf, 0, dstbuf.length);
    }

    /**
         * Read the requested number of bytes into a buffer.
         * <p>
         * This routine always reads until either the requested number of bytes
         * has been copied or EOF has been reached. Consequently callers do not
         * need to invoke it in a loop to make sure their buffer has been fully
         * populated.
         * </p>
         * 
         * @param position
         *                the starting offset, as measured in bytes from the
         *                beginning of this file, to copy from.
         * @param dstbuf
         *                buffer to copy the bytes into.
         * @param dstoff
         *                offset within <code>dstbuf</code> to start copying
         *                into.
         * @param cnt
         *                number of bytes to copy. Must not exceed
         *                <code>dstbuf.length - dstoff</code>.
         * @return total number of bytes read. Always <code>length</code>
         *         unless the requested range to copy is over the end of the
         *         file.
         * @throws IOException
         *                 a necessary window was not found in the window cache
         *                 and trying to load it in from the operating system
         *                 failed.
         */
    public int read(long position, final byte[] dstbuf, int dstoff,
	    final int cnt) throws IOException {
	int remaining = cnt;
	while (remaining > 0 && position < length) {
	    final int r = cache.get(wp, (int) (position >> szb)).copy(
		    ((int) position) & szm, dstbuf, dstoff, remaining);
	    position += r;
	    dstoff += r;
	    remaining -= r;
	}
	return cnt - remaining;
    }

    /**
         * Close this file and remove all open windows.
         * 
         * @throws IOException
         *                 the file refused to be closed.
         */
    public void close() throws IOException {
	cache.purge(wp);
	wp.fd.close();
    }

    private class Provider extends WindowProvider {
	final MapMode mapping;

	final RandomAccessFile fd;

	Provider(final MapMode m, final File file) throws IOException {
	    mapping = m;
	    fd = new RandomAccessFile(file,
		    m == null || m == MapMode.READ_ONLY ? "r" : "rw");
	}

	public ByteWindow loadWindow(final int windowId) throws IOException {
	    final int windowSize = getWindowSize(windowId);
	    if (mapping != null) {
		final MappedByteBuffer map;
		map = fd.getChannel().map(mapping, windowId << szb, windowSize);
		if (map.hasArray())
		    return new ByteArrayWindow(this, windowId, map.array());
		else
		    return new ByteBufferWindow(this, windowId, map);
	    }

	    final byte[] b = new byte[windowSize];
	    synchronized (fd) {
		fd.seek(windowId << szb);
		fd.readFully(b);
	    }
	    return new ByteArrayWindow(this, windowId, b);
	}

	public int getWindowSize(final int id) {
	    final long position = id << szb;
	    return length < position + sz ? (int) (length - position) : sz;
	}
    }
}
