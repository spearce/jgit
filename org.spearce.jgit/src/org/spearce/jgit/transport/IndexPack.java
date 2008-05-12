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
package org.spearce.jgit.transport;

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
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.BinaryDelta;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.MutableObjectId;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectIdMap;
import org.spearce.jgit.lib.ObjectLoader;
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

	/**
	 * Create an index pack instance to load a new pack into a repository.
	 * <p>
	 * The received pack data and generated index will be saved to temporary
	 * files within the repository's <code>objects</code> directory. To use
	 * the data contained within them call {@link #renameAndOpenPack()} once the
	 * indexing is complete.
	 * 
	 * @param db
	 *            the repository that will receive the new pack.
	 * @param is
	 *            stream to read the pack data from.
	 * @return a new index pack instance.
	 * @throws IOException
	 *             a temporary file could not be created.
	 */
	public static IndexPack create(final Repository db, final InputStream is)
			throws IOException {
		final String suffix = ".pack";
		final File objdir = db.getObjectsDirectory();
		final File tmp = File.createTempFile("incoming_", suffix, objdir);
		final String n = tmp.getName();
		final File base;

		base = new File(objdir, n.substring(0, n.length() - suffix.length()));
		return new IndexPack(db, is, base);
	}

	private final Repository repo;

	private final Inflater inflater;

	private final MessageDigest objectDigest;

	private final MutableObjectId tempObjectId;

	private InputStream in;

	private byte[] buf;

	private long bBase;

	private int bOffset;

	private int bAvail;

	private boolean fixThin;

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
	 * @param db
	 * @param src
	 * @param dstBase
	 * @throws IOException
	 *             the output packfile could not be created.
	 */
	public IndexPack(final Repository db, final InputStream src,
			final File dstBase) throws IOException {
		repo = db;
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
	 * Configure this index pack instance to make a thin pack complete.
	 * <p>
	 * Thin packs are sometimes used during network transfers to allow a delta
	 * to be sent without a base object. Such packs are not permitted on disk.
	 * They can be fixed by copying the base object onto the end of the pack.
	 * 
	 * @param fix
	 *            true to enable fixing a thin pack.
	 */
	public void setFixThin(final boolean fix) {
		fixThin = fix;
	}

	/**
	 * Consume data from the input stream until the packfile is indexed.
	 * 
	 * @param progress
	 *            progress feedback
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

				progress.beginTask(PROGRESS_DOWNLOAD, (int) objectCount);
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
					if (entryCount < objectCount) {
						if (!fixThin) {
							throw new IOException("pack has "
									+ (objectCount - entryCount)
									+ " unresolved deltas");
						}
						fixThinPack(progress);
					}
				}

				packDigest = null;
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

			if (dstPack != null)
				dstPack.setReadOnly();
			if (dstIdx != null)
				dstIdx.setReadOnly();
		} catch (IOException err) {
			if (dstPack != null)
				dstPack.delete();
			if (dstIdx != null)
				dstIdx.delete();
			throw err;
		}
	}

	private void resolveDeltas(final ProgressMonitor progress)
			throws IOException {
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
			resolveDeltas(oe.pos, Constants.OBJ_BAD, null, oe);
	}

	private void resolveDeltas(final long pos, int type, byte[] data,
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
		case Constants.OBJ_TREE:
		case Constants.OBJ_BLOB:
		case Constants.OBJ_TAG:
			type = typeCode;
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
			objectDigest.update(Constants.encodedTypeString(type));
			objectDigest.update((byte) ' ');
			objectDigest.update(Constants.encodeASCII(data.length));
			objectDigest.update((byte) 0);
			objectDigest.update(data);
			tempObjectId.fromRaw(objectDigest.digest(), 0);
			oe = new ObjectEntry(pos, tempObjectId);
			entries[entryCount++] = oe;
		}

		resolveChildDeltas(pos, type, data, oe);
	}

	private void resolveChildDeltas(final long pos, int type, byte[] data,
			ObjectEntry oe) throws IOException {
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

	private void fixThinPack(final ProgressMonitor progress) throws IOException {
		growEntries();

		final Deflater def = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
		long end = packOut.length() - 20;
		while (!baseById.isEmpty()) {
			final ObjectId baseId = baseById.keySet().iterator().next();
			final ObjectLoader ldr = repo.openObject(baseId);
			final byte[] data = ldr.getBytes();
			final int typeCode = ldr.getType();
			final ObjectEntry oe;

			oe = new ObjectEntry(end, baseId);
			entries[entryCount++] = oe;
			packOut.seek(end);
			writeWhole(def, typeCode, data);
			end = packOut.getFilePointer();

			resolveChildDeltas(oe.pos, typeCode, data, oe);
			if (progress.isCancelled())
				throw new IOException("Download cancelled during indexing");
		}
		def.end();

		fixHeaderFooter();
	}

	private void writeWhole(final Deflater def, final int typeCode,
			final byte[] data) throws IOException {
		int sz = data.length;
		int hdrlen = 0;
		buf[hdrlen++] = (byte) ((typeCode << 4) | sz & 15);
		sz >>>= 4;
		while (sz > 0) {
			buf[hdrlen - 1] |= 0x80;
			buf[hdrlen++] = (byte) (sz & 0x7f);
			sz >>>= 7;
		}
		packOut.write(buf, 0, hdrlen);
		def.reset();
		def.setInput(data);
		def.finish();
		while (!def.finished())
			packOut.write(buf, 0, def.deflate(buf));
	}

	private void fixHeaderFooter() throws IOException {
		packOut.seek(0);
		if (packOut.read(buf, 0, 12) != 12)
			throw new IOException("Cannot re-read pack header to fix count");
		NB.encodeInt32(buf, 8, entryCount);
		packOut.seek(0);
		packOut.write(buf, 0, 12);

		packDigest.reset();
		packDigest.update(buf, 0, 12);
		for (;;) {
			final int n = packOut.read(buf);
			if (n < 0)
				break;
			packDigest.update(buf, 0, n);
		}

		packcsum = packDigest.digest();
		packOut.write(packcsum);
	}

	private void growEntries() {
		final ObjectEntry[] ne;

		ne = new ObjectEntry[(int) objectCount + baseById.size()];
		System.arraycopy(entries, 0, ne, 0, entryCount);
		entries = ne;
	}

	private void writeIdx() throws IOException {
		Arrays.sort(entries, 0, entryCount);
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
		case Constants.OBJ_TREE:
		case Constants.OBJ_BLOB:
		case Constants.OBJ_TAG:
			whole(typeCode, pos, sz);
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

	private void whole(final int type, final long pos, final long sz)
			throws IOException {
		objectDigest.update(Constants.encodedTypeString(type));
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
	 * Rename the pack to it's final name and location and open it.
	 * <p>
	 * If the call completes successfully the repository this IndexPack instance
	 * was created with will have the objects in the pack available for reading
	 * and use, without needing to scan for packs.
	 * 
	 * @throws IOException
	 *             The pack could not be inserted into the repository's objects
	 *             directory. The pack no longer exists on disk, as it was
	 *             removed prior to throwing the exception to the caller.
	 */
	public void renameAndOpenPack() throws IOException {
		final MessageDigest d = Constants.newMessageDigest();
		final byte[] oeBytes = new byte[Constants.OBJECT_ID_LENGTH];
		for (int i = 0; i < entryCount; i++) {
			final ObjectEntry oe = entries[i];
			oe.copyRawTo(oeBytes, 0);
			d.update(oeBytes);
		}

		final ObjectId name = ObjectId.fromRaw(d.digest());
		final File packDir = new File(repo.getObjectsDirectory(), "pack");
		final File finalPack = new File(packDir, "pack-" + name + ".pack");
		final File finalIdx = new File(packDir, "pack-" + name + ".idx");

		if (finalPack.exists()) {
			// If the pack is already present we should never replace it.
			//
			cleanupTemporaryFiles();
			return;
		}

		if (!dstPack.renameTo(finalPack)) {
			cleanupTemporaryFiles();
			throw new IOException("Cannot move pack to " + finalPack);
		}

		if (!dstIdx.renameTo(finalIdx)) {
			cleanupTemporaryFiles();
			if (!finalPack.delete())
				finalPack.deleteOnExit();
			throw new IOException("Cannot move index to " + finalIdx);
		}

		try {
			repo.openPack(finalPack, finalIdx);
		} catch (IOException err) {
			finalPack.delete();
			finalIdx.delete();
			throw err;
		}
	}

	private void cleanupTemporaryFiles() {
		if (!dstIdx.delete())
			dstIdx.deleteOnExit();
		if (!dstPack.delete())
			dstPack.deleteOnExit();
	}
}
