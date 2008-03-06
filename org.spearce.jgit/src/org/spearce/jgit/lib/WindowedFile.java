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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

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

	/**
	 * Open a file for reading through window caching.
	 * 
	 * @param winCache
	 *            the cache this file will maintain its windows in. All windows
	 *            in the same cache will be considered for cache eviction, so
	 *            multiple files from the same Git repository probably should
	 *            use the same window cache.
	 * @param file
	 *            the file to open. The file will be opened for reading only,
	 *            unless {@link FileChannel.MapMode#READ_WRITE} or {@link FileChannel.MapMode#PRIVATE}
	 *            is given.
	 * @param windowSz
	 *            number of bytes within a window. This value must be a power of
	 *            2 and must be at least 4096, or one system page, whichever is
	 *            larger. If <code>mapType</code> is not null then this value
	 *            should be large (e.g. several MiBs) to amortize the high cost
	 *            of mapping the file.
	 * @param usemmap
	 *            indicates if the operating system mmap should be used for the
	 *            byte windows. False means don't use mmap, preferring to
	 *            allocate a byte[] in Java and reading the file data into the
	 *            array. True will use a read only mmap, however this requires
	 *            allocation of small temporary objects during every read.
	 * @throws IOException
	 *             the file could not be opened.
	 */
	public WindowedFile(final WindowCache winCache, final File file,
			final int windowSz, final boolean usemmap) throws IOException {
		cache = winCache;
		sz = windowSz;
		szb = bits(windowSz);
		szm = (1 << szb) - 1;
		wp = new Provider(usemmap, file);
	}

	/**
	 * Get the total number of bytes available in this file.
	 * 
	 * @return the number of bytes contained within this file.
	 */
	public long length() {
		return wp.length;
	}

	/**
	 * Get the path name of this file.
	 *
	 * @return the absolute path name of the file.
	 */
	public String getName() {
		return wp.getStoreDescription();
	}

	/**
	 * Read the bytes into a buffer until it is full.
	 * <p>
	 * This routine always reads until either the requested number of bytes has
	 * been copied or EOF has been reached. Consequently callers do not need to
	 * invoke it in a loop to make sure their buffer has been fully populated.
	 * </p>
	 * 
	 * @param position
	 *            the starting offset, as measured in bytes from the beginning
	 *            of this file, to copy from.
	 * @param dstbuf
	 *            buffer to copy the bytes into.
	 * @return total number of bytes read. Always <code>dstbuf.length</code>
	 *         unless the requested range to copy is over the end of the file.
	 * @throws IOException
	 *             a necessary window was not found in the window cache and
	 *             trying to load it in from the operating system failed.
	 */
	public int read(final long position, final byte[] dstbuf)
			throws IOException {
		return read(position, dstbuf, 0, dstbuf.length);
	}

	/**
	 * Read the requested number of bytes into a buffer.
	 * <p>
	 * This routine always reads until either the requested number of bytes has
	 * been copied or EOF has been reached. Consequently callers do not need to
	 * invoke it in a loop to make sure their buffer has been fully populated.
	 * </p>
	 * 
	 * @param position
	 *            the starting offset, as measured in bytes from the beginning
	 *            of this file, to copy from.
	 * @param dstbuf
	 *            buffer to copy the bytes into.
	 * @param dstoff
	 *            offset within <code>dstbuf</code> to start copying into.
	 * @param cnt
	 *            number of bytes to copy. Must not exceed
	 *            <code>dstbuf.length - dstoff</code>.
	 * @return total number of bytes read. Always <code>length</code> unless
	 *         the requested range to copy is over the end of the file.
	 * @throws IOException
	 *             a necessary window was not found in the window cache and
	 *             trying to load it in from the operating system failed.
	 */
	public int read(long position, final byte[] dstbuf, int dstoff,
			final int cnt) throws IOException {
		int remaining = cnt;
		while (remaining > 0 && position < wp.length) {
			final int r = cache.get(wp, (int) (position >> szb)).copy(
					((int) position) & szm, dstbuf, dstoff, remaining);
			position += r;
			dstoff += r;
			remaining -= r;
		}
		return cnt - remaining;
	}

	/**
	 * Read the bytes into a buffer until it is full.
	 * <p>
	 * This routine always reads until either the requested number of bytes has
	 * been copied or EOF has been reached. Consequently callers do not need to
	 * invoke it in a loop to make sure their buffer has been fully populated.
	 * </p>
	 * 
	 * @param position
	 *            the starting offset, as measured in bytes from the beginning
	 *            of this file, to copy from.
	 * @param dstbuf
	 *            buffer to copy the bytes into.
	 * @throws IOException
	 *             a necessary window was not found in the window cache and
	 *             trying to load it in from the operating system failed.
	 * @throws EOFException
	 *             the file ended before <code>dstbuf.length</code> bytes
	 *             could be read.
	 */
	public void readFully(final long position, final byte[] dstbuf)
			throws IOException {
		if (read(position, dstbuf, 0, dstbuf.length) != dstbuf.length)
			throw new EOFException();
	}

	void readCompressed(final long position, final byte[] dstbuf)
			throws IOException, DataFormatException {
		final Inflater inf = cache.borrowInflater();
		try {
			readCompressed(position, dstbuf, inf);
		} finally {
			inf.reset();
			cache.returnInflater(inf);
		}
	}

	void readCompressed(long pos, final byte[] dstbuf, final Inflater inf)
			throws IOException, DataFormatException {
		int dstoff = 0;
		dstoff = cache.get(wp, (int) (pos >> szb)).inflate(((int) pos) & szm,
				dstbuf, dstoff, inf);
		pos >>= szb;
		while (!inf.finished()) {
			dstoff = cache.get(wp, (int) ++pos).inflate(0, dstbuf, dstoff, inf);
		}
		if (dstoff != dstbuf.length)
			throw new EOFException();
	}

	/**
	 * Reads a 32 bit unsigned integer in network byte order.
	 * 
	 * @param position
	 *            the starting offset, as measured in bytes from the beginning
	 *            of this file, to read from. The position does not need to be
	 *            aligned.
	 * @param intbuf
	 *            a temporary buffer to read the bytes into before byteorder
	 *            conversion. Must be supplied by the caller and must have room
	 *            for at least 4 bytes, but may be longer if the caller has a
	 *            larger buffer they wish to loan.
	 * @return the unsigned 32 bit integer value.
	 * @throws IOException
	 *             necessary window was not found in the window cache and trying
	 *             to load it in from the operating system failed.
	 * @throws EOFException
	 *             the file has less than 4 bytes remaining at position.
	 */
	public long readUInt32(final long position, byte[] intbuf)
			throws IOException {
		if (read(position, intbuf, 0, 4) != 4)
			throw new EOFException();
		return (intbuf[0] & 0xff) << 24 | (intbuf[1] & 0xff) << 16
				| (intbuf[2] & 0xff) << 8 | (intbuf[3] & 0xff);
	}

	/**
	 * Close this file and remove all open windows.
	 * 
	 * @throws IOException
	 *             the file refused to be closed.
	 */
	public void close() throws IOException {
		cache.purge(wp);
		wp.fd.close();
	}

	private class Provider extends WindowProvider {
		final boolean map;

		final RandomAccessFile fd;

		long length;

		final File fPath;

		Provider(final boolean usemmap, final File file) throws IOException {
			map = usemmap;
			fd = new RandomAccessFile(file, "r");
			length = fd.length();
			fPath = file;
		}

		public ByteWindow loadWindow(final int windowId) throws IOException {
			final int windowSize = getWindowSize(windowId);
			if (map) {
				final MappedByteBuffer map = fd.getChannel().map(
						MapMode.READ_ONLY, windowId << szb, windowSize);
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

		@Override
		public String getStoreDescription() {
			return fPath.getAbsolutePath();
		}
	}
}
