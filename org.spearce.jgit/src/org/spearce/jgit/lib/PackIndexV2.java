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
import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.util.NB;

/** Support for the pack index v2 format. */
class PackIndexV2 extends PackIndex {
	private static final long IS_O64 = 1L << 31;

	private static final int FANOUT = 256;

	private static final int[] NO_INTS = {};

	private static final byte[] NO_BYTES = {};

	private long objectCnt;

	/** 256 arrays of contiguous object names. */
	private int[][] names;

	/** 256 arrays of the 32 bit offset data, matching {@link #names}. */
	private byte[][] offset32;

	/** 64 bit offset table. */
	private byte[] offset64;

	PackIndexV2(final InputStream fd) throws IOException {
		final byte[] fanoutRaw = new byte[4 * FANOUT];
		NB.readFully(fd, fanoutRaw, 0, fanoutRaw.length);
		final long[] fanoutTable = new long[FANOUT];
		for (int k = 0; k < FANOUT; k++)
			fanoutTable[k] = NB.decodeUInt32(fanoutRaw, k * 4);
		objectCnt = fanoutTable[FANOUT - 1];

		names = new int[FANOUT][];
		offset32 = new byte[FANOUT][];

		// Object name table. The size we can permit per fan-out bucket
		// is limited to Java's 2 GB per byte array limitation. That is
		// no more than 107,374,182 objects per fan-out.
		//
		for (int k = 0; k < FANOUT; k++) {
			final long bucketCnt;
			if (k == 0)
				bucketCnt = fanoutTable[k];
			else
				bucketCnt = fanoutTable[k] - fanoutTable[k - 1];

			if (bucketCnt == 0) {
				names[k] = NO_INTS;
				offset32[k] = NO_BYTES;
				continue;
			}

			final long nameLen = bucketCnt * Constants.OBJECT_ID_LENGTH;
			if (nameLen > Integer.MAX_VALUE)
				throw new IOException("Index file is too large for jgit");

			final int intNameLen = (int) nameLen;
			final byte[] raw = new byte[intNameLen];
			final int[] bin = new int[intNameLen >> 2];
			NB.readFully(fd, raw, 0, raw.length);
			for (int i = 0; i < bin.length; i++)
				bin[i] = NB.decodeInt32(raw, i << 2);

			names[k] = bin;
			offset32[k] = new byte[(int) (bucketCnt * 4)];
		}

		// CRC32 table. Currently unused.
		//
		skipFully(fd, objectCnt * 4);

		// 32 bit offset table. Any entries with the most significant bit
		// set require a 64 bit offset entry in another table.
		//
		int o64cnt = 0;
		for (int k = 0; k < FANOUT; k++) {
			final byte[] ofs = offset32[k];
			NB.readFully(fd, ofs, 0, ofs.length);
			for (int p = 0; p < ofs.length; p += 4)
				if (ofs[p] < 0)
					o64cnt++;
		}

		// 64 bit offset table. Most objects should not require an entry.
		//
		if (o64cnt > 0) {
			offset64 = new byte[o64cnt * 8];
			NB.readFully(fd, offset64, 0, offset64.length);
		} else {
			offset64 = NO_BYTES;
		}
	}

	private static void skipFully(final InputStream fd, long toSkip)
			throws IOException {
		while (toSkip > 0) {
			final long r = fd.skip(toSkip);
			if (r <= 0)
				throw new EOFException("Cannot skip index section.");
			toSkip -= r;
		}
	}

	@Override
	long getObjectCount() {
		return objectCnt;
	}

	@Override
	long findOffset(final AnyObjectId objId) {
		final int levelOne = objId.getFirstByte();
		final int[] data = names[levelOne];
		int high = offset32[levelOne].length >> 2;
		if (high == 0)
			return -1;
		int low = 0;
		do {
			final int mid = (low + high) >> 1;
			final int mid4 = mid << 2;
			final int cmp;

			cmp = objId.compareTo(data, mid4 + mid); // mid * 5
			if (cmp < 0)
				high = mid;
			else if (cmp == 0) {
				final long p = NB.decodeUInt32(offset32[levelOne], mid4);
				if ((p & IS_O64) != 0)
					return NB.decodeUInt64(offset64, (8 * (int) (p & ~IS_O64)));
				return p;
			} else
				low = mid + 1;
		} while (low < high);
		return -1;
	}
}
