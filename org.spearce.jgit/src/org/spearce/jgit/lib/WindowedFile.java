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
	private final File fPath;

	final int hash;

	private RandomAccessFile fd;

	private long length;

	/** Total number of windows actively in the associated cache. */
	int openCount;

	/**
	 * Open a file for reading through window caching.
	 * 
	 * @param file
	 *            the file to open.
	 */
	public WindowedFile(final File file) {
		fPath = file;
		hash = System.identityHashCode(this);
		length = Long.MAX_VALUE;
	}

	/**
	 * Get the total number of bytes available in this file.
	 * 
	 * @return the number of bytes contained within this file.
	 */
	public long length() {
		return length;
	}

	/**
	 * Get the path name of this file.
	 * 
	 * @return the absolute path name of the file.
	 */
	public String getName() {
		return fPath.getAbsolutePath();
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
	 * @param curs
	 *            current cursor for reading data from the file.
	 * @return total number of bytes read. Always <code>dstbuf.length</code>
	 *         unless the requested range to copy is over the end of the file.
	 * @throws IOException
	 *             a necessary window was not found in the window cache and
	 *             trying to load it in from the operating system failed.
	 */
	public int read(final long position, final byte[] dstbuf,
			final WindowCursor curs) throws IOException {
		return read(position, dstbuf, 0, dstbuf.length, curs);
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
	 * @param curs
	 *            current cursor for reading data from the file.
	 * @return total number of bytes read. Always <code>length</code> unless
	 *         the requested range to copy is over the end of the file.
	 * @throws IOException
	 *             a necessary window was not found in the window cache and
	 *             trying to load it in from the operating system failed.
	 */
	public int read(long position, final byte[] dstbuf, int dstoff,
			final int cnt, final WindowCursor curs) throws IOException {
		int remaining = cnt;
		while (remaining > 0 && position < length) {
			final int r = curs.copy(this, (int) (position >> WindowCache.szb),
					((int) position) & WindowCache.szm, dstbuf, dstoff,
					remaining);
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
	 * @param curs
	 *            current cursor for reading data from the file.
	 * @throws IOException
	 *             a necessary window was not found in the window cache and
	 *             trying to load it in from the operating system failed.
	 * @throws EOFException
	 *             the file ended before <code>dstbuf.length</code> bytes
	 *             could be read.
	 */
	public void readFully(final long position, final byte[] dstbuf,
			final WindowCursor curs) throws IOException {
		if (read(position, dstbuf, 0, dstbuf.length, curs) != dstbuf.length)
			throw new EOFException();
	}

	void readCompressed(final long position, final byte[] dstbuf,
			final WindowCursor curs) throws IOException, DataFormatException {
		final Inflater inf = InflaterCache.get();
		try {
			readCompressed(position, dstbuf, curs, inf);
		} finally {
			InflaterCache.release(inf);
		}
	}

	void readCompressed(long pos, final byte[] dstbuf, final WindowCursor curs,
			final Inflater inf) throws IOException, DataFormatException {
		int dstoff = 0;
		dstoff = curs.inflate(this, (int) (pos >> WindowCache.szb), ((int) pos)
				& WindowCache.szm, dstbuf, dstoff, inf);
		pos >>= WindowCache.szb;
		while (!inf.finished())
			dstoff = curs.inflate(this, (int) ++pos, 0, dstbuf, dstoff, inf);
		if (dstoff != dstbuf.length)
			throw new EOFException();
	}

	/**
	 * Overridable hook called after the file is opened.
	 * <p>
	 * This hook is invoked each time the file is opened for reading, but before
	 * the first window is mapped into the cache. Implementers are free to use
	 * any of the window access methods to obtain data, however doing so may
	 * pollute the window cache with otherwise unnecessary windows.
	 * </p>
	 * 
	 * @throws IOException
	 *             something is wrong with the file, for example the caller does
	 *             not understand its version header information.
	 */
	protected void onOpen() throws IOException {
		// Do nothing by default.
	}

	/** Close this file and remove all open windows. */
	public void close() {
		WindowCache.purge(this);
	}

	void cacheOpen() throws IOException {
		fd = new RandomAccessFile(fPath, "r");
		length = fd.length();
		try {
			onOpen();
		} catch (IOException ioe) {
			cacheClose();
			throw ioe;
		} catch (RuntimeException re) {
			cacheClose();
			throw re;
		} catch (Error re) {
			cacheClose();
			throw re;
		}
	}

	void cacheClose() {
		try {
			fd.close();
		} catch (IOException err) {
			// Ignore a close event. We had it open only for reading.
			// There should not be errors related to network buffers
			// not flushed, etc.
		}
		fd = null;
	}

	void loadWindow(final WindowCursor curs, final int windowId)
			throws IOException {
		final long position = windowId << WindowCache.szb;
		final int windowSize = getWindowSize(windowId);
		if (WindowCache.mmap) {
			final MappedByteBuffer map = fd.getChannel().map(MapMode.READ_ONLY,
					position, windowSize);
			if (map.hasArray()) {
				final byte[] b = map.array();
				curs.window = new ByteArrayWindow(this, windowId, b);
				curs.handle = b;
			} else {
				curs.window = new ByteBufferWindow(this, windowId, map);
				curs.handle = map;
			}
			return;
		}

		final byte[] b = new byte[windowSize];
		synchronized (fd) {
			fd.seek(position);
			fd.readFully(b);
		}
		curs.window = new ByteArrayWindow(this, windowId, b);
		curs.handle = b;
	}

	int getWindowSize(final int id) {
		final int sz = WindowCache.sz;
		final long position = id << WindowCache.szb;
		return length < position + sz ? (int) (length - position) : sz;
	}
}
