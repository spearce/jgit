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

import java.io.File;
import java.io.IOException;

import org.spearce.jgit.errors.CorruptObjectException;

class PackIndex {
	private static final int IDX_HDR_LEN = 256 * 4;

	private byte[][] idxdata;

	PackIndex(final File idxFile, final long objectCnt) throws IOException {
		// FIXME window size and mmap type should be configurable
		final WindowedFile idx = new WindowedFile(new WindowCache(8*1024*1024,1), idxFile, 8*1024*1024, true);
		try {
			loadVersion1(idx, objectCnt);
		} finally {
			try {
				idx.close();
			} catch (IOException err2) {
				// ignore
			}
		}
	}

	private void loadVersion1(final WindowedFile idx, final long objectCnt) throws CorruptObjectException, IOException {
		if (idx.length() != (IDX_HDR_LEN + (24 * objectCnt) + (2 * Constants.OBJECT_ID_LENGTH)))
			throw new CorruptObjectException("Invalid pack index"
					+ ", incorrect file length: " + idx.getName());

		final long[] idxHeader = new long[256]; // really unsigned 32-bit...
		final byte[] intbuf = new byte[4];
		for (int k = 0; k < idxHeader.length; k++)
			idxHeader[k] = idx.readUInt32(k * 4, intbuf);
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
				int off = (int) ((k == 0) ? 0 : idxHeader[k-1] * (Constants.OBJECT_ID_LENGTH + 4));
				idx.read(off + IDX_HDR_LEN, idxdata[k]);
			}
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
