/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

import org.spearce.jgit.errors.CorruptObjectException;

public class PackFile {
    private static final int IDX_HDR_LEN = 256 * 4;

    private static final byte[] SIGNATURE = { 'P', 'A', 'C', 'K' };

    private final Repository repo;

    private final WindowedFile pack;

    private final WindowedFile idx;

    private final long[] idxHeader;

    private long objectCnt;

    public PackFile(final Repository parentRepo, final File packFile)
	    throws IOException {
	repo = parentRepo;
	// FIXME window size and mmap type should be configurable
	pack = new WindowedFile(repo.getWindowCache(), packFile,
		16 * 1024 * 1024, null);
	try {
	    readPackHeader();

	    final String name = packFile.getName();
	    final int dot = name.lastIndexOf('.');
	    final File idxFile = new File(packFile.getParentFile(), name
		    .substring(0, dot)
		    + ".idx");
	    // FIXME window size and mmap type should be configurable
	    idx = new WindowedFile(repo.getWindowCache(), idxFile,
		    16 * 1024 * 1024, null);
	    try {
		idxHeader = readIndexHeader();
	    } catch (IOException ioe) {
		try {
		    idx.close();
		} catch (IOException err2) {
		}
		throw ioe;
	    }
	} catch (IOException ioe) {
	    try {
		pack.close();
	    } catch (IOException err2) {
	    }
	    throw ioe;
	}
    }

    ObjectReader resolveBase(final ObjectId id) throws IOException {
	return get(id);
    }

    ObjectReader resolveBase(final long ofs) throws IOException {
	return reader(ofs);
    }

    public synchronized PackedObjectReader get(final ObjectId id)
	    throws IOException {
	final long offset = findOffset(id);
	if (offset == -1)
	    return null;
	final PackedObjectReader objReader = reader(offset);
	objReader.setId(id);
	return objReader;
    }

    public void close() throws IOException {
	pack.close();
	idx.close();
    }

    final int read(final long pos, final byte[] dst, final int off, final int n)
	    throws IOException {
	return pack.read(pos, dst, off, n);
    }

    private void readPackHeader() throws IOException {
	long position = 0;
	final byte[] sig = new byte[SIGNATURE.length];
	final byte[] intbuf = new byte[4];
	final long vers;

	if (pack.read(position, sig) != SIGNATURE.length)
	    throw new IOException("Not a PACK file.");
	for (int k = 0; k < SIGNATURE.length; k++) {
	    if (sig[k] != SIGNATURE[k])
		throw new IOException("Not a PACK file.");
	}
	position += SIGNATURE.length;

	vers = pack.readUInt32(position, intbuf);
	if (vers != 2 && vers != 3)
	    throw new IOException("Unsupported pack version " + vers + ".");
	position += 4;

	objectCnt = pack.readUInt32(position, intbuf);
    }

    private long[] readIndexHeader() throws CorruptObjectException, IOException {
	if (idx.length() != (IDX_HDR_LEN + (24 * objectCnt) + (2 * Constants.OBJECT_ID_LENGTH)))
	    throw new CorruptObjectException("Invalid pack index");

	final long[] idxHeader = new long[256];
	final byte[] intbuf = new byte[4];
	for (int k = 0; k < idxHeader.length; k++)
	    idxHeader[k] = idx.readUInt32(k * 4, intbuf);
	return idxHeader;
    }

    private PackedObjectReader reader(final long objOffset) throws IOException {
	final byte[] ib = new byte[Constants.OBJECT_ID_LENGTH];
	long pos = objOffset;
	int c;

	pack.readFully(pos, ib);
	c = ib[(int) (pos++ - objOffset)] & 0xff;
	final int typeCode = (c >> 4) & 7;
	long size = c & 15;
	int shift = 4;
	while ((c & 0x80) != 0) {
	    c = ib[(int) (pos++ - objOffset)] & 0xff;
	    size += (c & 0x7f) << shift;
	    shift += 7;
	}

	switch (typeCode) {
	case Constants.OBJ_EXT:
	    throw new IOException("Extended object types not supported.");
	case Constants.OBJ_COMMIT:
	    return new WholePackedObjectReader(this, pos,
		    Constants.TYPE_COMMIT, size);
	case Constants.OBJ_TREE:
	    return new WholePackedObjectReader(this, pos, Constants.TYPE_TREE,
		    size);
	case Constants.OBJ_BLOB:
	    return new WholePackedObjectReader(this, pos, Constants.TYPE_BLOB,
		    size);
	case Constants.OBJ_TAG:
	    return new WholePackedObjectReader(this, pos, Constants.TYPE_TAG,
		    size);
	case Constants.OBJ_TYPE_5:
	    throw new IOException("Object type 5 not supported.");
	case Constants.OBJ_OFS_DELTA: {
	    pack.readFully(pos, ib);
	    c = ib[(int) (pos++ - objOffset)] & 0xff;
	    long ofs = c & 127;
	    while ((c & 128) != 0) {
		ofs += 1;
		c = ib[(int) (pos++ - objOffset)] & 0xff;
		ofs <<= 7;
		ofs += (c & 127);
	    }
	    return new DeltaOfsPackedObjectReader(this, pos, objOffset - ofs);
	}
	case Constants.OBJ_REF_DELTA: {
	    pack.readFully(pos, ib);
	    pos += ib.length;
	    return new DeltaRefPackedObjectReader(this, pos, new ObjectId(ib));
	}
	default:
	    throw new IOException("Unknown object type " + typeCode + ".");
	}
    }

    private long findOffset(final ObjectId objId) throws IOException {
	final int levelOne = objId.getFirstByte();
	final byte[] intbuf = new byte[4];
	final byte[] tmpid = new byte[Constants.OBJECT_ID_LENGTH];
	long high = idxHeader[levelOne];
	long low = levelOne == 0 ? 0 : idxHeader[levelOne - 1];

	do {
	    final long mid = (low + high) / 2;
	    final long pos = IDX_HDR_LEN
		    + ((4 + Constants.OBJECT_ID_LENGTH) * mid) + 4;
	    idx.readFully(pos, tmpid);
	    final int cmp = objId.compareTo(tmpid);
	    if (cmp < 0)
		high = mid;
	    else if (cmp == 0)
		return idx.readUInt32(pos - 4, intbuf);
	    else
		low = mid + 1;
	} while (low < high);
	return -1;
    }
}
