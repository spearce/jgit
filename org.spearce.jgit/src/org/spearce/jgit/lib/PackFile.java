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

import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

import org.spearce.jgit.util.NB;

/**
 * A Git version 2 pack file representation. A pack file contains
 * Git objects in delta packed format yielding high compression of
 * lots of object where some objects are similar.
 */
public class PackFile {
	private static final byte[] SIGNATURE = { 'P', 'A', 'C', 'K' };

	private final WindowedFile pack;

	private final PackIndex idx;

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
		pack = new WindowedFile(packFile) {
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
	}

	final PackedObjectLoader resolveBase(final WindowCursor curs, final long ofs)
			throws IOException {
		return reader(curs, ofs);
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
	public boolean hasObject(final AnyObjectId id) {
		return idx.hasObject(id);
	}

	/**
	 * Get an object from this pack.
	 * 
	 * @param id
	 *            the object to obtain from the pack. Must not be null.
	 * @return the object loader for the requested object if it is contained in
	 *         this pack; null if the object was not found.
	 * @throws IOException
	 *             the pack file or the index could not be read.
	 */
	public PackedObjectLoader get(final AnyObjectId id) throws IOException {
		return get(new WindowCursor(), id);
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
		final long offset = idx.findOffset(id);
		if (offset == -1)
			return null;
		final PackedObjectLoader objReader = reader(curs, offset);
		objReader.setId(id.toObjectId());
		return objReader;
	}

	/**
	 * Close the resources utilized by this repository
	 */
	public void close() {
		UnpackedObjectCache.purge(pack);
		pack.close();
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

		pack.readFully(position, intbuf, curs);
		vers = NB.decodeUInt32(intbuf, 0);
		if (vers != 2 && vers != 3)
			throw new IOException("Unsupported pack version " + vers + ".");
		position += 4;

		pack.readFully(position, intbuf, curs);
		final long objectCnt = NB.decodeUInt32(intbuf, 0);
		if (idx.getObjectCount() != objectCnt)
			throw new IOException("Pack index"
					+ " object count mismatch; expected " + objectCnt
					+ " found " + idx.getObjectCount() + ": "
					+ pack.getName());
	}

	private PackedObjectLoader reader(final WindowCursor curs,
			final long objOffset)
			throws IOException {
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
			return new WholePackedObjectLoader(curs, this, pos, typeCode,
					(int) dataSize);

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
					(int) dataSize, objOffset - ofs);
		}
		case Constants.OBJ_REF_DELTA: {
			pack.readFully(pos, ib, curs);
			return new DeltaRefPackedObjectLoader(curs, this, pos + ib.length,
					(int) dataSize, ObjectId.fromRaw(ib));
		}
		default:
			throw new IOException("Unknown object type " + typeCode + ".");
		}
	}
}
