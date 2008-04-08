/*
 *  Copyright (C) 2007  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.fetch;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.BinaryDelta;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.MutableObjectId;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectIdMap;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.util.NB;

/** Indexes Git pack files for local use. */
public class IndexPack {
	/** Progress message when reading raw data from the pack. */
	public static final String PROGRESS_DOWNLOAD = "Receiving objects";

	/** Progress message when computing names of delta compressed objects. */
	public static final String PROGRESS_RESOLVE_DELTA = "Resolving deltas";

	private static final byte[] SIGNATURE = { 'P', 'A', 'C', 'K' };

	private static final int BUFFER_SIZE = 2048;

	private static final byte[] COMMIT;

	private static final byte[] TREE;

	private static final byte[] TAG;

	private static final byte[] BLOB;

	static {
		COMMIT = Constants.encodeASCII(Constants.TYPE_COMMIT);
		TREE = Constants.encodeASCII(Constants.TYPE_TREE);
		TAG = Constants.encodeASCII(Constants.TYPE_TAG);
		BLOB = Constants.encodeASCII(Constants.TYPE_BLOB);
	}

	private final Inflater inflater;

	private final MessageDigest objectDigest;

	private final MutableObjectId tempObjectId;

	private InputStream in;

	private byte[] buf;

	private long bBase;

	private int bOffset;

	private int bAvail;

	private final File dstPack;

	private final File dstIdx;

	private long objectCount;

	private ObjectEntry[] entries;

	private int deltaCount;

	private int entryCount;

	private ObjectIdMap<ArrayList<UnresolvedDelta>> baseById;

	private HashMap<Long, ArrayList<UnresolvedDelta>> baseByPos;

	private byte[] objectData;

	private MessageDigest packDigest;

	private RandomAccessFile packOut;

	private byte[] packcsum;

	/**
	 * Create a new pack indexer utility.
	 *
	 * @param src
	 * @param dstBase
	 * @throws IOException
	 *             the output packfile could not be created.
	 */
	public IndexPack(final InputStream src, final File dstBase)
			throws IOException {
		in = src;
		inflater = new Inflater(false);
		buf = new byte[BUFFER_SIZE];
		objectData = new byte[BUFFER_SIZE];
		objectDigest = Constants.newMessageDigest();
		tempObjectId = new MutableObjectId();
		packDigest = Constants.newMessageDigest();

		if (dstBase != null) {
			final File dir = dstBase.getParentFile();
			final String nam = dstBase.getName();
			dstPack = new File(dir, nam + ".pack");
			dstIdx = new File(dir, nam + ".idx");
			packOut = new RandomAccessFile(dstPack, "rw");
			packOut.setLength(0);
		} else {
			dstPack = null;
			dstIdx = null;
		}
	}

	/**
	 * Consume data from the input stream until the packfile is indexed.
	 * @param progress progress feedback
	 *
	 * @throws IOException
	 */
	public void index(final ProgressMonitor progress) throws IOException {
		progress.start(2 /* tasks */);
		try {
			try {
				readPackHeader();

				entries = new ObjectEntry[(int) objectCount];
				baseById = new ObjectIdMap<ArrayList<UnresolvedDelta>>();
				baseByPos = new HashMap<Long, ArrayList<UnresolvedDelta>>();

				progress.beginTask(PROGRESS_DOWNLOAD, (int)objectCount);
				for (int done = 0; done < objectCount; done++) {
					indexOneObject();
					progress.update(1);
					if (progress.isCancelled())
						throw new IOException("Download cancelled");
				}
				readPackFooter();
				endInput();
				progress.endTask();
				if (deltaCount > 0) {
					if (packOut == null)
						throw new IOException("need packOut");
					resolveDeltas(progress);
					if (entryCount < objectCount)
						throw new IOException("thin packs aren't supported");
				}
				baseById = null;
				baseByPos = null;

				if (dstIdx != null)
					writeIdx();

			} finally {
				progress.endTask();
				if (packOut != null)
					packOut.close();
				inflater.end();
			}
		} catch (IOException err) {
			if (dstPack != null)
				dstPack.delete();
			if (dstIdx != null)
				dstIdx.delete();
			throw err;
		}
	}

	private void resolveDeltas(final ProgressMonitor progress) throws IOException {
		progress.beginTask(PROGRESS_RESOLVE_DELTA, deltaCount);
		final int last = entryCount;
		for (int i = 0; i < last; i++) {
			final int before = entryCount;
			resolveDeltas(entries[i]);
			progress.update(entryCount - before);
			if (progress.isCancelled())
				throw new IOException("Download cancelled during indexing");
		}
		progress.endTask();
	}

	private void resolveDeltas(final ObjectEntry oe) throws IOException {
		if (baseById.containsKey(oe) || baseByPos.containsKey(new Long(oe.pos)))
			resolveDeltas(oe.pos, null, null, oe);
	}

	private void resolveDeltas(final long pos, byte[] type, byte[] data,
			ObjectEntry oe) throws IOException {
		position(pos);
		int c = readFromFile();
		final int typeCode = (c >> 4) & 7;
		long sz = c & 15;
		int shift = 4;
		while ((c & 0x80) != 0) {
			c = readFromFile();
			sz += (c & 0x7f) << shift;
			shift += 7;
		}

		switch (typeCode) {
		case Constants.OBJ_COMMIT:
			type = COMMIT;
			data = inflateFromFile((int) sz);
			break;
		case Constants.OBJ_TREE:
			type = TREE;
			data = inflateFromFile((int) sz);
			break;
		case Constants.OBJ_BLOB:
			type = BLOB;
			data = inflateFromFile((int) sz);
			break;
		case Constants.OBJ_TAG:
			type = TAG;
			data = inflateFromFile((int) sz);
			break;
		case Constants.OBJ_OFS_DELTA: {
			c = readFromInput() & 0xff;
			while ((c & 128) != 0)
				c = readFromInput() & 0xff;
			data = BinaryDelta.apply(data, inflateFromFile((int) sz));
			break;
		}
		case Constants.OBJ_REF_DELTA: {
			fillFromInput(20);
			use(20);
			data = BinaryDelta.apply(data, inflateFromFile((int) sz));
			break;
		}
		default:
			throw new IOException("Unknown object type " + typeCode + ".");
		}

		if (oe == null) {
			objectDigest.update(type);
			objectDigest.update((byte) ' ');
			objectDigest.update(Constants.encodeASCII(data.length));
			objectDigest.update((byte) 0);
			objectDigest.update(data);
			tempObjectId.fromRaw(objectDigest.digest(), 0);
			oe = new ObjectEntry(pos, tempObjectId);
			entries[entryCount++] = oe;
		}

		final ArrayList<UnresolvedDelta> a = baseById.remove(oe);
		final ArrayList<UnresolvedDelta> b = baseByPos.remove(new Long(pos));
		int ai = 0, bi = 0;
		if (a != null && b != null) {
			while (ai < a.size() && bi < b.size()) {
				final UnresolvedDelta ad = a.get(ai);
				final UnresolvedDelta bd = b.get(bi);
				if (ad.position < bd.position) {
					resolveDeltas(ad.position, type, data, null);
					ai++;
				} else {
					resolveDeltas(bd.position, type, data, null);
					bi++;
				}
			}
		}
		if (a != null)
			while (ai < a.size())
				resolveDeltas(a.get(ai++).position, type, data, null);
		if (b != null)
			while (bi < b.size())
				resolveDeltas(b.get(bi++).position, type, data, null);
	}

	private void writeIdx() throws IOException {
		Arrays.sort(entries);
		final int[] fanout = new int[256];
		for (int i = 0; i < entryCount; i++)
			fanout[entries[i].getFirstByte() & 0xff]++;
		for (int i = 1; i < 256; i++)
			fanout[i] += fanout[i - 1];

		final BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(dstIdx), BUFFER_SIZE);
		try {
			final byte[] rawoe = new byte[4 + Constants.OBJECT_ID_LENGTH];
			final MessageDigest d = Constants.newMessageDigest();
			for (int i = 0; i < 256; i++) {
				NB.encodeInt32(rawoe, 0, fanout[i]);
				os.write(rawoe, 0, 4);
				d.update(rawoe, 0, 4);
			}
			for (int i = 0; i < entryCount; i++) {
				final ObjectEntry oe = entries[i];
				if (oe.pos >>> 1 > Integer.MAX_VALUE)
					throw new IOException("Pack too large for index version 1");
				NB.encodeInt32(rawoe, 0, (int) oe.pos);
				oe.copyRawTo(rawoe, 4);
				os.write(rawoe);
				d.update(rawoe);
			}
			os.write(packcsum);
			d.update(packcsum);
			os.write(d.digest());
		} finally {
			os.close();
		}
	}

	private void readPackHeader() throws IOException {
		final int hdrln = SIGNATURE.length + 4 + 4;
		final int p = fillFromInput(hdrln);
		for (int k = 0; k < SIGNATURE.length; k++)
			if (buf[p + k] != SIGNATURE[k])
				throw new IOException("Not a PACK file.");

		final long vers = NB.decodeUInt32(buf, p + 4);
		if (vers != 2 && vers != 3)
			throw new IOException("Unsupported pack version " + vers + ".");
		objectCount = NB.decodeUInt32(buf, p + 8);
		use(hdrln);
	}

	private void readPackFooter() throws IOException {
		sync();
		final byte[] cmpcsum = packDigest.digest();
		final int c = fillFromInput(20);
		packcsum = new byte[20];
		System.arraycopy(buf, c, packcsum, 0, 20);
		use(20);
		if (packOut != null)
			packOut.write(packcsum);

		if (!Arrays.equals(cmpcsum, packcsum))
			throw new CorruptObjectException("Packfile checksum incorrect.");
	}

	// Cleanup all resources associated with our input parsing.
	private void endInput() {
		in = null;
		packDigest = null;
		objectData = null;
	}

	// Read one entire object or delta from the input.
	private void indexOneObject() throws IOException {
		final long pos = position();

		int c = readFromInput();
		final int typeCode = (c >> 4) & 7;
		long sz = c & 15;
		int shift = 4;
		while ((c & 0x80) != 0) {
			c = readFromInput();
			sz += (c & 0x7f) << shift;
			shift += 7;
		}

		switch (typeCode) {
		case Constants.OBJ_COMMIT:
			whole(COMMIT, pos, sz);
			break;
		case Constants.OBJ_TREE:
			whole(TREE, pos, sz);
			break;
		case Constants.OBJ_BLOB:
			whole(BLOB, pos, sz);
			break;
		case Constants.OBJ_TAG:
			whole(TAG, pos, sz);
			break;
		case Constants.OBJ_OFS_DELTA: {
			c = readFromInput() & 0xff;
			long ofs = c & 127;
			while ((c & 128) != 0) {
				ofs += 1;
				c = readFromInput() & 0xff;
				ofs <<= 7;
				ofs += (c & 127);
			}
			final Long base = new Long(pos - ofs);
			ArrayList<UnresolvedDelta> r = baseByPos.get(base);
			if (r == null) {
				r = new ArrayList<UnresolvedDelta>(8);
				baseByPos.put(base, r);
			}
			r.add(new UnresolvedDelta(pos));
			deltaCount++;
			inflateFromInput(false);
			break;
		}
		case Constants.OBJ_REF_DELTA: {
			c = fillFromInput(20);
			final byte[] ref = new byte[20];
			System.arraycopy(buf, c, ref, 0, 20);
			use(20);
			final ObjectId base = ObjectId.fromRaw(ref);
			ArrayList<UnresolvedDelta> r = baseById.get(base);
			if (r == null) {
				r = new ArrayList<UnresolvedDelta>(8);
				baseById.put(base, r);
			}
			r.add(new UnresolvedDelta(pos));
			deltaCount++;
			inflateFromInput(false);
			break;
		}
		default:
			throw new IOException("Unknown object type " + typeCode + ".");
		}
	}

	private void whole(final byte[] type, final long pos, final long sz)
			throws IOException {
		objectDigest.update(type);
		objectDigest.update((byte) ' ');
		objectDigest.update(Constants.encodeASCII(sz));
		objectDigest.update((byte) 0);
		inflateFromInput(true);
		tempObjectId.fromRaw(objectDigest.digest(), 0);
		entries[entryCount++] = new ObjectEntry(pos, tempObjectId);
	}

	// Current position of {@link #bOffset} within the entire file.
	private long position() {
		return bBase + bOffset;
	}

	private void position(final long pos) throws IOException {
		packOut.seek(pos);
		bBase = pos;
		bOffset = 0;
		bAvail = 0;
	}

	// Consume exactly one byte from the buffer and return it.
	private int readFromInput() throws IOException {
		if (bAvail == 0)
			fillFromInput(1);
		bAvail--;
		return buf[bOffset++] & 0xff;
	}

	// Consume exactly one byte from the buffer and return it.
	private int readFromFile() throws IOException {
		if (bAvail == 0)
			fillFromFile();
		bAvail--;
		return buf[bOffset++] & 0xff;
	}

	// Consume cnt bytes from the buffer.
	private void use(final int cnt) {
		bOffset += cnt;
		bAvail -= cnt;
	}

	// Ensure at least need bytes are available in in {@link #buf}.
	private int fillFromInput(final int need) throws IOException {
		while (bAvail < need) {
			int next = bOffset + bAvail;
			int free = buf.length - next;
			if (free + bAvail < need) {
				sync();
				next = bAvail;
				free = buf.length - next;
			}
			next = in.read(buf, next, free);
			if (next <= 0)
				throw new EOFException("Packfile is truncated.");
			bAvail += next;
		}
		return bOffset;
	}

	// Ensure at least need bytes are available in in {@link #buf}.
	private int fillFromFile() throws IOException {
		if (bAvail == 0) {
			final int next = packOut.read(buf, 0, buf.length);
			if (next <= 0)
				throw new EOFException("Packfile is truncated.");
			bAvail = next;
			bOffset = 0;
		}
		return bOffset;
	}

	// Store consumed bytes in {@link #buf} up to {@link #bOffset}.
	private void sync() throws IOException {
		packDigest.update(buf, 0, bOffset);
		if (packOut != null)
			packOut.write(buf, 0, bOffset);
		if (bAvail > 0)
			System.arraycopy(buf, bOffset, buf, 0, bAvail);
		bBase += bOffset;
		bOffset = 0;
	}

	private void inflateFromInput(final boolean digest) throws IOException {
		final Inflater inf = inflater;
		try {
			final byte[] dst = objectData;
			int n = 0;
			while (!inf.finished()) {
				if (inf.needsInput()) {
					final int p = fillFromInput(1);
					inf.setInput(buf, p, bAvail);
					use(bAvail);
				}

				int free = dst.length - n;
				if (free < 8) {
					if (digest)
						objectDigest.update(dst, 0, n);
					n = 0;
					free = dst.length;
				}

				n += inf.inflate(dst, n, free);
			}
			if (digest)
				objectDigest.update(dst, 0, n);
			use(-inf.getRemaining());
		} catch (DataFormatException dfe) {
			throw corrupt(dfe);
		} finally {
			inf.reset();
		}
	}

	private byte[] inflateFromFile(final int sz) throws IOException {
		final Inflater inf = inflater;
		try {
			final byte[] dst = new byte[sz];
			int n = 0;
			while (!inf.finished()) {
				if (inf.needsInput()) {
					final int p = fillFromFile();
					inf.setInput(buf, p, bAvail);
					use(bAvail);
				}
				n += inf.inflate(dst, n, sz - n);
			}
			use(-inf.getRemaining());
			return dst;
		} catch (DataFormatException dfe) {
			throw corrupt(dfe);
		} finally {
			inf.reset();
		}
	}

	private static CorruptObjectException corrupt(final DataFormatException dfe) {
		return new CorruptObjectException("Packfile corruption detected: "
				+ dfe.getMessage());
	}

	private static class ObjectEntry extends ObjectId {
		final long pos;

		ObjectEntry(final long headerOffset, final AnyObjectId id) {
			super(id);
			pos = headerOffset;
		}
	}

	private static class UnresolvedDelta {
		final long position;

		UnresolvedDelta(final long headerOffset) {
			position = headerOffset;
		}
	}

	/**
	 * Rename the temporary pack to it's final name and location.
	 *
	 * @param db
	 * @throws IOException
	 */
	public void renamePack(Repository db) throws IOException {
		final MessageDigest d = Constants.newMessageDigest();
		final byte[] oeBytes = new byte[Constants.OBJECT_ID_LENGTH];
		for (int i = 0; i < entryCount; i++) {
			final ObjectEntry oe = entries[i];
			oe.copyRawTo(oeBytes, 0);
			d.update(oeBytes);
		}
		ObjectId name = ObjectId.fromRaw(d.digest());
		System.out.println("name pack "+name);
		File packDir = new File(db.getObjectsDirectory(),"pack");
		File finalPack = new File(packDir, "pack-"+name+".pack");
		File finalIdx = new File(packDir, "pack-"+name+".idx");
		if (!dstIdx.renameTo(finalIdx)) {
			if (!dstIdx.delete())
				dstIdx.deleteOnExit();
			throw new IOException("Cannot rename final pack index");
		}
		if (!dstPack.renameTo(finalPack)) {
			if (!finalIdx.delete())
				finalIdx.deleteOnExit();
			if (!dstPack.delete())
				dstPack.deleteOnExit();
			throw new IOException("Cannot rename final pack");
		}
	}
}
