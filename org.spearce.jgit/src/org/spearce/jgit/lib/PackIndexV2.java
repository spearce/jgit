package org.spearce.jgit.lib;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

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
		readFully(fd, 0, fanoutRaw);
		final long[] fanoutTable = new long[FANOUT];
		for (int k = 0; k < FANOUT; k++)
			fanoutTable[k] = decodeUInt32(k * 4, fanoutRaw);
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
			readFully(fd, 0, raw);
			for (int i = 0; i < bin.length; i++)
				bin[i] = ObjectId.rawUInt32(raw, i << 2);

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
			readFully(fd, 0, ofs);
			for (int p = 0; p < ofs.length; p += 4)
				if (ofs[p] < 0)
					o64cnt++;
		}

		// 64 bit offset table. Most objects should not require an entry.
		//
		if (o64cnt > 0) {
			offset64 = new byte[o64cnt * 8];
			readFully(fd, 0, offset64);
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
	long findOffset(final ObjectId objId) {
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
				final long p = decodeUInt32(mid4, offset32[levelOne]);
				if ((p & IS_O64) != 0)
					return decodeUInt64((8 * (int) (p & ~IS_O64)), offset64);
				return p;
			} else
				low = mid + 1;
		} while (low < high);
		return -1;
	}
}
