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
import java.util.zip.DataFormatException;

/**
 * A Git version 2 pack file representation. A pack file contains
 * Git objects in delta packed format yielding high compression of
 * lots of object where some objects are similar.
 */
public class PackFile {
	private static final byte[] SIGNATURE = { 'P', 'A', 'C', 'K' };

	private final Repository repo;

	private final WindowedFile pack;

	private final PackIndex idx;

	private final UnpackedObjectCache deltaBaseCache;

	/**
	 * Construct a reader for an existing, pre-indexed packfile.
	 *
	 * @param parentRepo
	 *            Git repository holding this pack file
	 * @param idxFile
	 *            path of the <code>.idx</code> file listing the contents.
	 * @param packFile
	 *            path of the <code>.pack</code> file holding the data.
	 * @throws IOException
	 *             the index file cannot be accessed at this time.
	 */
	public PackFile(final Repository parentRepo, final File idxFile,
			final File packFile) throws IOException {
		repo = parentRepo;
		pack = new WindowedFile(repo.getWindowCache(), packFile) {
			@Override
			protected void onOpen() throws IOException {
				readPackHeader();
			}
		};
		try {
			idx = PackIndex.open(idxFile);
		} catch (IOException ioe) {
			throw ioe;
		}
		deltaBaseCache = pack.cache.deltaBaseCache;
	}

	PackedObjectLoader resolveBase(final long ofs) throws IOException {
		return reader(ofs);
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
	 */
	public boolean hasObject(final ObjectId id) {
		return idx.findOffset(id) != -1;
	}

	/**
	 * Get an object from this pack.
	 * <p>
	 * For performance reasons the caller is responsible for supplying a
	 * temporary buffer of at least {@link Constants#OBJECT_ID_LENGTH} bytes for
	 * use during searching. If an object loader is returned this temporary
	 * buffer becomes the property of the object loader and must not be
	 * overwritten by the caller. If no object loader is returned then the
	 * temporary buffer remains the property of the caller and may be given to a
	 * different pack file to continue searching for the needed object.
	 * </p>
	 * 
	 * @param id
	 *            the object to obtain from the pack. Must not be null.
	 * @return the object loader for the requested object if it is contained in
	 *         this pack; null if the object was not found.
	 * @throws IOException
	 *             the pack file or the index could not be read.
	 */
	public PackedObjectLoader get(final ObjectId id)
			throws IOException {
		final long offset = idx.findOffset(id);
		if (offset == -1)
			return null;
		final PackedObjectLoader objReader = reader(offset);
		objReader.setId(id);
		return objReader;
	}

	/**
	 * Close the resources utilized by this repository
	 */
	public void close() {
		deltaBaseCache.purge(pack);
		pack.close();
	}

	UnpackedObjectCache.Entry readCache(final long position) {
		return deltaBaseCache.get(pack, position);
	}

	void saveCache(final long position, final byte[] data, final String type) {
		deltaBaseCache.store(pack, position, data, type);
	}

	byte[] decompress(final long position, final int totalSize)
			throws DataFormatException, IOException {
		final byte[] dstbuf = new byte[totalSize];
		pack.readCompressed(position, dstbuf, new WindowCursor());
		return dstbuf;
	}

	private void readPackHeader() throws IOException {
		final WindowCursor curs = new WindowCursor();
		long position = 0;
		final byte[] sig = new byte[SIGNATURE.length];
		final byte[] intbuf = new byte[4];
		final long vers;

		if (pack.read(position, sig, curs) != SIGNATURE.length)
			throw new IOException("Not a PACK file.");
		for (int k = 0; k < SIGNATURE.length; k++) {
			if (sig[k] != SIGNATURE[k])
				throw new IOException("Not a PACK file.");
		}
		position += SIGNATURE.length;

		vers = pack.readUInt32(position, intbuf, curs);
		if (vers != 2 && vers != 3)
			throw new IOException("Unsupported pack version " + vers + ".");
		position += 4;

		final long objectCnt = pack.readUInt32(position, intbuf, curs);
		if (idx.getObjectCount() != objectCnt)
			throw new IOException("Pack index"
					+ " object count mismatch; expected " + objectCnt
					+ " found " + idx.getObjectCount() + ": "
					+ pack.getName());
	}

	private PackedObjectLoader reader(final long objOffset)
			throws IOException {
		long pos = objOffset;
		int p = 0;
		final WindowCursor curs = new WindowCursor();
		final byte[] ib = new byte[Constants.OBJECT_ID_LENGTH];
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
			return whole(Constants.TYPE_COMMIT, pos, dataSize);
		case Constants.OBJ_TREE:
			return whole(Constants.TYPE_TREE, pos, dataSize);
		case Constants.OBJ_BLOB:
			return whole(Constants.TYPE_BLOB, pos, dataSize);
		case Constants.OBJ_TAG:
			return whole(Constants.TYPE_TAG, pos, dataSize);
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
			return new DeltaOfsPackedObjectLoader(this, pos + p,
					(int) dataSize, objOffset - ofs);
		}
		case Constants.OBJ_REF_DELTA: {
			pack.readFully(pos, ib, curs);
			return new DeltaRefPackedObjectLoader(this, pos + ib.length,
					(int) dataSize, new ObjectId(ib));
		}
		default:
			throw new IOException("Unknown object type " + typeCode + ".");
		}
	}

	private final WholePackedObjectLoader whole(final String type,
			final long pos, final long size) {
		return new WholePackedObjectLoader(this, pos, type, (int) size);
	}
}
