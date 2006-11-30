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
import java.nio.channels.FileChannel.MapMode;
import java.util.zip.DataFormatException;

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
		64 * 1024 * 1024, MapMode.READ_WRITE);
	try {
	    readPackHeader();

	    final String name = packFile.getName();
	    final int dot = name.lastIndexOf('.');
	    final File idxFile = new File(packFile.getParentFile(), name
		    .substring(0, dot)
		    + ".idx");
	    // FIXME window size and mmap type should be configurable
	    idx = new WindowedFile(repo.getWindowCache(), idxFile,
		    64 * 1024 * 1024, MapMode.READ_WRITE);
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

    ObjectLoader resolveBase(final ObjectId id) throws IOException {
	return get(id);
    }

    ObjectLoader resolveBase(final long ofs) throws IOException {
	return reader(ofs);
    }

    public synchronized PackedObjectLoader get(final ObjectId id)
	    throws IOException {
	final long offset = findOffset(id);
	if (offset == -1)
	    return null;
	final PackedObjectLoader objReader = reader(offset);
	objReader.setId(id);
	return objReader;
    }

    public void close() throws IOException {
	pack.close();
	idx.close();
    }

    byte[] decompress(final long position, final int totalSize)
	    throws DataFormatException, IOException {
	final byte[] dstbuf = new byte[totalSize];
	pack.readCompressed(position, dstbuf);
	return dstbuf;
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

    private PackedObjectLoader reader(final long objOffset) throws IOException {
	final byte[] ib = new byte[Constants.OBJECT_ID_LENGTH];
	long pos = objOffset;
	int p = 0;

	pack.readFully(pos, ib);
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
	    pack.readFully(pos, ib);
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
	    pack.readFully(pos, ib);
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
