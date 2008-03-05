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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.errors.CorruptObjectException;

class PackIndex {
	private static final int IDX_HDR_LEN = 256 * 4;

	private byte[][] idxdata;

	PackIndex(final File idxFile, final long objectCnt) throws IOException {
		final FileInputStream fd = new FileInputStream(idxFile);
		try {
			loadVersion1(fd, objectCnt, idxFile);
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

	private void loadVersion1(final InputStream fd, final long objectCnt,
			final File idxFile) throws CorruptObjectException, IOException {
		if (idxFile.length() != (IDX_HDR_LEN + (24 * objectCnt) + (2 * Constants.OBJECT_ID_LENGTH)))
			throw new CorruptObjectException("Invalid pack index v1 length.");

		final byte[] fanoutTable = new byte[IDX_HDR_LEN];
		readFully(fd, fanoutTable);

		final long[] idxHeader = new long[256]; // really unsigned 32-bit...
		for (int k = 0; k < idxHeader.length; k++)
			idxHeader[k] = decodeUInt32(k * 4, fanoutTable);
		idxdata = new byte[idxHeader.length][];
		for (int k = 0; k < idxHeader.length; k++) {
			int n;
			if (k == 0) {
				n = (int)(idxHeader[k]);
			} else {
				n = (int)(idxHeader[k]-idxHeader[k-1]);
			}
			if (n > 0) {
				idxdata[k] = new byte[n * (Constants.OBJECT_ID_LENGTH + 4)];
				readFully(fd, idxdata[k]);
			}
		}
	}

	private static long decodeUInt32(final int offset, final byte[] intbuf) {
		return (intbuf[offset + 0] & 0xff) << 24
				| (intbuf[offset + 1] & 0xff) << 16
				| (intbuf[offset + 2] & 0xff) << 8
				| (intbuf[offset + 3] & 0xff);
	}

	private static void readFully(final InputStream fd, final byte[] buf)
			throws IOException {
		int dstoff = 0;
		int remaining = buf.length;
		while (remaining > 0) {
			final int r = fd.read(buf, dstoff, remaining);
			if (r <= 0)
				throw new EOFException("Short read of index data block.");
			dstoff += r;
			remaining -= r;
		}
	}

	long findOffset(final ObjectId objId) {
		final int levelOne = objId.getFirstByte();
		byte[] data = idxdata[levelOne];
		if (data == null)
			return -1;
		long high = data.length / (4 + Constants.OBJECT_ID_LENGTH);
		long low = 0;
		do {
			final long mid = (low + high) / 2;
			final long pos = ((4 + Constants.OBJECT_ID_LENGTH) * mid) + 4;
			final int cmp = objId.compareTo(data, pos);
			if (cmp < 0)
				high = mid;
			else if (cmp == 0) {
				int b0 = data[(int)pos-4] & 0xff;
				int b1 = data[(int)pos-3] & 0xff;
				int b2 = data[(int)pos-2] & 0xff;
				int b3 = data[(int)pos-1] & 0xff;
				return (((long)b0) << 24) | ( b1 << 16 ) | ( b2 << 8 ) | (b3);
			} else
				low = mid + 1;
		} while (low < high);
		return -1;
	}
}
