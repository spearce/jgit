/*
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.DataFormatException;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.util.NB;

/**
 * A Git version 2 pack file representation. A pack file contains Git objects in
 * delta packed format yielding high compression of lots of object where some
 * objects are similar.
 */
public class PackFile implements Iterable<PackIndex.MutableEntry> {
	/** Sorts PackFiles to be most recently created to least recently created. */
	public static Comparator<PackFile> SORT = new Comparator<PackFile>() {
		public int compare(final PackFile a, final PackFile b) {
			return b.packLastModified - a.packLastModified;
		}
	};

	private final File idxFile;

	private final WindowedFile pack;

	private int packLastModified;

	private PackIndex loadedIdx;

	private PackReverseIndex reverseIdx;

	/**
	 * Construct a reader for an existing, pre-indexed packfile.
	 * 
	 * @param idxFile
	 *            path of the <code>.idx</code> file listing the contents.
	 * @param packFile
	 *            path of the <code>.pack</code> file holding the data.
	 */
	public PackFile(final File idxFile, final File packFile) {
		this.idxFile = idxFile;
		this.packLastModified = (int) (packFile.lastModified() >> 10);
		pack = new WindowedFile(packFile) {
			@Override
			protected void onOpen() throws IOException {
				onOpenPack();
			}
		};
	}

	private synchronized PackIndex idx() throws IOException {
		if (loadedIdx == null) {
			loadedIdx = PackIndex.open(idxFile);
		}
		return loadedIdx;
	}

	final PackedObjectLoader resolveBase(final WindowCursor curs, final long ofs)
			throws IOException {
		return reader(curs, ofs);
	}

	/** @return the File object which locates this pack on disk. */
	public File getPackFile() {
		return pack.getFile();
	}

	/**
	 * Determine if an object is contained within the pack file.
	 * <p>
	 * For performance reasons only the index file is searched; the main pack
	 * content is ignored entirely.
	 * </p>
	 * 
	 * @param id
	 *            the object to look for. Must not be null.
	 * @return true if the object is in this pack; false otherwise.
	 * @throws IOException
	 *             the index file cannot be loaded into memory.
	 */
	public boolean hasObject(final AnyObjectId id) throws IOException {
		return idx().hasObject(id);
	}

	/**
	 * Get an object from this pack.
	 * 
	 * @param curs
	 *            temporary working space associated with the calling thread.
	 * @param id
	 *            the object to obtain from the pack. Must not be null.
	 * @return the object loader for the requested object if it is contained in
	 *         this pack; null if the object was not found.
	 * @throws IOException
	 *             the pack file or the index could not be read.
	 */
	public PackedObjectLoader get(final WindowCursor curs, final AnyObjectId id)
			throws IOException {
		final long offset = idx().findOffset(id);
		return 0 < offset ? reader(curs, offset) : null;
	}

	/**
	 * Close the resources utilized by this repository
	 */
	public void close() {
		UnpackedObjectCache.purge(pack);
		pack.close();
		synchronized (this) {
			loadedIdx = null;
		}
	}

	/**
	 * Provide iterator over entries in associated pack index, that should also
	 * exist in this pack file. Objects returned by such iterator are mutable
	 * during iteration.
	 * <p>
	 * Iterator returns objects in SHA-1 lexicographical order.
	 * </p>
	 * 
	 * @return iterator over entries of associated pack index
	 * 
	 * @see PackIndex#iterator()
	 */
	public Iterator<PackIndex.MutableEntry> iterator() {
		try {
			return idx().iterator();
		} catch (IOException e) {
			return Collections.<PackIndex.MutableEntry> emptyList().iterator();
		}
	}

	/**
	 * Obtain the total number of objects available in this pack. This method
	 * relies on pack index, giving number of effectively available objects.
	 * 
	 * @return number of objects in index of this pack, likewise in this pack
	 * @throws IOException
	 *             the index file cannot be loaded into memory.
	 */
	long getObjectCount() throws IOException {
		return idx().getObjectCount();
	}

	/**
	 * Search for object id with the specified start offset in associated pack
	 * (reverse) index.
	 *
	 * @param offset
	 *            start offset of object to find
	 * @return object id for this offset, or null if no object was found
	 * @throws IOException
	 *             the index file cannot be loaded into memory.
	 */
	ObjectId findObjectForOffset(final long offset) throws IOException {
		return getReverseIdx().findObject(offset);
	}

	final UnpackedObjectCache.Entry readCache(final long position) {
		return UnpackedObjectCache.get(pack, position);
	}

	final void saveCache(final long position, final byte[] data, final int type) {
		UnpackedObjectCache.store(pack, position, data, type);
	}

	final byte[] decompress(final long position, final int totalSize,
			final WindowCursor curs) throws DataFormatException, IOException {
		final byte[] dstbuf = new byte[totalSize];
		pack.readCompressed(position, dstbuf, curs);
		return dstbuf;
	}

	final void copyRawData(final PackedObjectLoader loader,
			final OutputStream out, final byte buf[]) throws IOException {
		final long objectOffset = loader.objectOffset;
		final long dataOffset = loader.dataOffset;
		final int cnt = (int) (findEndOffset(objectOffset) - dataOffset);
		final WindowCursor curs = loader.curs;
		final PackIndex idx = idx();

		if (idx.hasCRC32Support()) {
			final CRC32 crc = new CRC32();
			int headerCnt = (int) (dataOffset - objectOffset);
			while (headerCnt > 0) {
				int toRead = Math.min(headerCnt, buf.length);
				int read = pack.read(objectOffset, buf, 0, toRead, curs);
				if (read != toRead)
					throw new EOFException();
				crc.update(buf, 0, read);
				headerCnt -= toRead;
			}
			final CheckedOutputStream crcOut = new CheckedOutputStream(out, crc);
			pack.copyToStream(dataOffset, buf, cnt, crcOut, curs);
			final long computed = crc.getValue();

			final ObjectId id = findObjectForOffset(objectOffset);
			final long expected = idx.findCRC32(id);
			if (computed != expected)
				throw new CorruptObjectException("Object at " + dataOffset
						+ " in " + getPackFile() + " has bad zlib stream");
		} else {
			try {
				pack.verifyCompressed(dataOffset, curs);
			} catch (DataFormatException dfe) {
				final CorruptObjectException coe;
				coe = new CorruptObjectException("Object at " + dataOffset
						+ " in " + getPackFile() + " has bad zlib stream");
				coe.initCause(dfe);
				throw coe;
			}
			pack.copyToStream(dataOffset, buf, cnt, out, curs);
		}
	}

	boolean supportsFastCopyRawData() throws IOException {
		return idx().hasCRC32Support();
	}

	private void onOpenPack() throws IOException {
		final PackIndex idx = idx();
		final WindowCursor curs = new WindowCursor();
		long position = 0;
		final byte[] sig = new byte[Constants.PACK_SIGNATURE.length];
		final byte[] intbuf = new byte[4];
		final long vers;

		if (pack.read(position, sig, curs) != Constants.PACK_SIGNATURE.length)
			throw new IOException("Not a PACK file.");
		for (int k = 0; k < Constants.PACK_SIGNATURE.length; k++) {
			if (sig[k] != Constants.PACK_SIGNATURE[k])
				throw new IOException("Not a PACK file.");
		}
		position += Constants.PACK_SIGNATURE.length;

		pack.readFully(position, intbuf, curs);
		vers = NB.decodeUInt32(intbuf, 0);
		if (vers != 2 && vers != 3)
			throw new IOException("Unsupported pack version " + vers + ".");
		position += 4;

		pack.readFully(position, intbuf, curs);
		final long packCnt = NB.decodeUInt32(intbuf, 0);
		final long idxCnt = idx.getObjectCount();
		if (idxCnt != packCnt)
			throw new IOException("Pack index"
					+ " object count mismatch; expected " + packCnt
					+ " found " + idxCnt + ": " + pack.getName());

		final byte[] csumbuf = new byte[20];
		pack.readFully(pack.length() - 20, csumbuf, curs);
		if (!Arrays.equals(csumbuf, idx.packChecksum))
			throw new IOException("Pack index mismatch; pack SHA-1 is "
					+ ObjectId.fromRaw(csumbuf).name() + ", index expects "
					+ ObjectId.fromRaw(idx.packChecksum).name());
	}

	private PackedObjectLoader reader(final WindowCursor curs,
			final long objOffset) throws IOException {
		long pos = objOffset;
		int p = 0;
		final byte[] ib = curs.tempId;
		pack.readFully(pos, ib, curs);
		int c = ib[p++] & 0xff;
		final int typeCode = (c >> 4) & 7;
		long dataSize = c & 15;
		int shift = 4;
		while ((c & 0x80) != 0) {
			c = ib[p++] & 0xff;
			dataSize += (c & 0x7f) << shift;
			shift += 7;
		}
		pos += p;

		switch (typeCode) {
		case Constants.OBJ_COMMIT:
		case Constants.OBJ_TREE:
		case Constants.OBJ_BLOB:
		case Constants.OBJ_TAG:
			return new WholePackedObjectLoader(curs, this, pos, objOffset,
					typeCode, (int) dataSize);

		case Constants.OBJ_OFS_DELTA: {
			pack.readFully(pos, ib, curs);
			p = 0;
			c = ib[p++] & 0xff;
			long ofs = c & 127;
			while ((c & 128) != 0) {
				ofs += 1;
				c = ib[p++] & 0xff;
				ofs <<= 7;
				ofs += (c & 127);
			}
			return new DeltaOfsPackedObjectLoader(curs, this, pos + p,
					objOffset, (int) dataSize, objOffset - ofs);
		}
		case Constants.OBJ_REF_DELTA: {
			pack.readFully(pos, ib, curs);
			return new DeltaRefPackedObjectLoader(curs, this, pos + ib.length,
					objOffset, (int) dataSize, ObjectId.fromRaw(ib));
		}
		default:
			throw new IOException("Unknown object type " + typeCode + ".");
		}
	}

	private long findEndOffset(final long startOffset)
			throws IOException, CorruptObjectException {
		final long maxOffset = pack.length() - Constants.OBJECT_ID_LENGTH;
		return getReverseIdx().findNextOffset(startOffset, maxOffset);
	}

	private synchronized PackReverseIndex getReverseIdx() throws IOException {
		if (reverseIdx == null)
			reverseIdx = new PackReverseIndex(idx());
		return reverseIdx;
	}
}
