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

import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.errors.CorruptObjectException;

class PackIndexV1 extends PackIndex {
	private static final int IDX_HDR_LEN = 256 * 4;

	private byte[][] idxdata;

	private long objectCnt;

	PackIndexV1(final InputStream fd, final byte[] hdr)
			throws CorruptObjectException, IOException {
		final byte[] fanoutTable = new byte[IDX_HDR_LEN];
		System.arraycopy(hdr, 0, fanoutTable, 0, hdr.length);
		readFully(fd, hdr.length, fanoutTable);

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
				readFully(fd, 0, idxdata[k]);
			}
		}
		objectCnt = idxHeader[255];
	}

	long getObjectCount() {
		return objectCnt;
	}

	long findOffset(final ObjectId objId) {
		final int levelOne = objId.getFirstByte();
		byte[] data = idxdata[levelOne];
		if (data == null)
			return -1;
		int high = data.length / (4 + Constants.OBJECT_ID_LENGTH);
		int low = 0;
		do {
			final int mid = (low + high) / 2;
			final int pos = ((4 + Constants.OBJECT_ID_LENGTH) * mid) + 4;
			final int cmp = objId.compareTo(data, pos);
			if (cmp < 0)
				high = mid;
			else if (cmp == 0) {
				int b0 = data[pos-4] & 0xff;
				int b1 = data[pos-3] & 0xff;
				int b2 = data[pos-2] & 0xff;
				int b3 = data[pos-1] & 0xff;
				return (((long)b0) << 24) | ( b1 << 16 ) | ( b2 << 8 ) | (b3);
			} else
				low = mid + 1;
		} while (low < high);
		return -1;
	}
}
