/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.lib;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.zip.DataFormatException;

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

	RandomAccessFile fd;

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

		// Multiply by 31 here so we can more directly combine with another
		// value in WindowCache.hash(), without doing the multiply there.
		//
		hash = System.identityHashCode(this) * 31;
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

	/** @return the absolute file object this file reads from. */
	public File getFile() {
		return fPath.getAbsoluteFile();
	}

	/**
	 * Get the path name of this file.
	 * 
	 * @return the absolute path name of the file.
	 */
	public String getName() {
		return getFile().getPath();
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
		return curs.copy(this, position, dstbuf, dstoff, cnt);
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

	/**
	 * Copy the requested number of bytes to the provided output stream.
	 * <p>
	 * This routine always reads until either the requested number of bytes has
	 * been copied or EOF has been reached.
	 * </p>
	 *
	 * @param position
	 *            the starting offset, as measured in bytes from the beginning
	 *            of this file, to copy from.
	 * @param buf
	 *            temporary buffer to copy bytes into. In case of a big amount
	 *            of data to copy, size of at least few kB is recommended. It
	 *            does not need to be of size <code>cnt</code>, however.
	 * @param cnt
	 *            number of bytes to copy. Must not exceed
	 *            <code>file.length - position</code>.
	 * @param out
	 *            output stream where read data is written out. No buffering is
	 *            guaranteed by this method.
	 * @param curs
	 *            current cursor for reading data from the file.
	 * @throws IOException
	 *             a necessary window was not found in the window cache and
	 *             trying to load it in from the operating system failed.
	 * @throws EOFException
	 *             the file ended before <code>cnt</code> bytes could be read.
	 */
	public void copyToStream(long position, final byte[] buf, long cnt,
			final OutputStream out, final WindowCursor curs)
			throws IOException, EOFException {
		while (cnt > 0) {
			int toRead = (int) Math.min(cnt, buf.length);
			int read = read(position, buf, 0, toRead, curs);
			if (read != toRead)
				throw new EOFException();
			position += read;
			cnt -= read;
			out.write(buf, 0, read);
		}
	}

	void readCompressed(final long position, final byte[] dstbuf,
			final WindowCursor curs) throws IOException, DataFormatException {
		if (curs.inflate(this, position, dstbuf, 0) != dstbuf.length)
			throw new EOFException("Short compressed stream at " + position);
	}

	void verifyCompressed(final long position, final WindowCursor curs)
			throws IOException, DataFormatException {
		curs.inflateVerify(this, position);
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

	void allocWindow(final WindowCursor curs, final int windowId,
			final long pos, final int size) {
		if (WindowCache.mmap) {
			MappedByteBuffer map;
			try {
				map = fd.getChannel().map(MapMode.READ_ONLY, pos, size);
			} catch (IOException e) {
				// The most likely reason this failed is the JVM has run out
				// of virtual memory. We need to discard quickly, and try to
				// force the GC to finalize and release any existing mappings.
				try {
					curs.release();
					System.gc();
					System.runFinalization();
					map = fd.getChannel().map(MapMode.READ_ONLY, pos, size);
				} catch (IOException ioe2) {
					// Temporarily disable mmap and do buffered disk IO.
					//
					map = null;
					System.err.println("warning: mmap failure: "+ioe2);
				}
			}
			if (map != null) {
				if (map.hasArray()) {
					final byte[] b = map.array();
					final ByteArrayWindow w;
					w = new ByteArrayWindow(this, pos, windowId, b);
					w.loaded = true;
					curs.window = w;
					curs.handle = b;
				} else {
					curs.window = new ByteBufferWindow(this, pos, windowId, map);
					curs.handle = map;
				}
				return;
			}
		}

		final byte[] b = new byte[size];
		curs.window = new ByteArrayWindow(this, pos, windowId, b);
		curs.handle = b;
		openCount++; // Until the window loads, we must stay open.
	}
}
