/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Access path to locate objects by {@link ObjectId} in a {@link PackFile}.
 * <p>
 * Indexes are strictly redundant information in that we can rebuild all of the
 * data held in the index file from the on disk representation of the pack file
 * itself, but it is faster to access for random requests because data is stored
 * by ObjectId.
 * </p>
 */
abstract class PackIndex {
	/**
	 * Open an existing pack <code>.idx</code> file for reading.
	 * <p>
	 * The format of the file will be automatically detected and a proper access
	 * implementation for that format will be constructed and returned to the
	 * caller. The file may or may not be held open by the returned instance.
	 * </p>
	 *
	 * @param idxFile
	 *            existing pack .idx to read.
	 * @return access implementation for the requested file.
	 * @throws FileNotFoundException
	 *             the file does not exist.
	 * @throws IOException
	 *             the file exists but could not be read due to security errors,
	 *             unrecognized data version, or unexpected data corruption.
	 */
	static PackIndex open(final File idxFile) throws IOException {
		final FileInputStream fd = new FileInputStream(idxFile);
		try {
			final byte[] hdr = new byte[8];
			readFully(fd, 0, hdr);
			return new PackIndexV1(fd, hdr);
		} catch (IOException ioe) {
			final String path = idxFile.getAbsolutePath();
			final IOException err;
			err = new IOException("Unreadable pack index: " + path);
			err.initCause(ioe);
			throw err;
		} finally {
			try {
				fd.close();
			} catch (IOException err2) {
				// ignore
			}
		}
	}

	/**
	 * Convert sequence of 4 bytes (network byte order) into unsigned value.
	 *
	 * @param offset
	 *            position within the buffer to begin reading from. This
	 *            position and the next 3 bytes after it (for a total of 4
	 *            bytes) will be read.
	 * @param intbuf
	 *            buffer to acquire the 4 bytes of data from.
	 * @return unsigned integer value that matches the 32 bits read.
	 */
	protected static long decodeUInt32(final int offset, final byte[] intbuf) {
		return (intbuf[offset + 0] & 0xff) << 24
				| (intbuf[offset + 1] & 0xff) << 16
				| (intbuf[offset + 2] & 0xff) << 8
				| (intbuf[offset + 3] & 0xff);
	}

	/**
	 * Read the entire byte array into memory, or throw an exception.
	 *
	 * @param fd
	 *            input stream to read the data from.
	 * @param dstoff
	 *            position within the buffer to start writing to.
	 * @param buf
	 *            buffer that must be fully populated, from dstoff to its end.
	 * @throws EOFException
	 *             the stream ended before buf was fully populated.
	 * @throws IOException
	 *             there was an error reading from the stream.
	 */
	protected static void readFully(final InputStream fd, int dstoff,
			final byte[] buf) throws IOException {
		int remaining = buf.length - dstoff;
		while (remaining > 0) {
			final int r = fd.read(buf, dstoff, remaining);
			if (r <= 0)
				throw new EOFException("Short read of index data block.");
			dstoff += r;
			remaining -= r;
		}
	}

	/**
	 * Obtain the total number of objects described by this index.
	 *
	 * @return number of objects in this index, and likewise in the associated
	 *         pack that this index was generated from.
	 */
	abstract long getObjectCount();

	/**
	 * Locate the file offset position for the requested object.
	 *
	 * @param objId
	 *            name of the object to locate within the pack.
	 * @return offset of the object's header and compressed content; -1 if the
	 *         object does not exist in this index and is thus not stored in the
	 *         associated pack.
	 */
	abstract long findOffset(ObjectId objId);
}
