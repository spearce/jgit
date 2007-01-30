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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.spearce.jgit.errors.CorruptObjectException;

public class UnpackedObjectLoader extends ObjectLoader {
	private final String objectType;

	private final int objectSize;

	private final byte[] bytes;

	public UnpackedObjectLoader(final Repository db, final ObjectId id)
			throws IOException {
		final FileInputStream objStream = new FileInputStream(db.toFile(id));
		final byte[] compressed;
		try {
			compressed = new byte[objStream.available()];
			int off = 0;
			while (off < compressed.length)
				off += objStream.read(compressed, off, compressed.length - off);
		} finally {
			objStream.close();
		}
		setId(id);

		// Try to determine if this is a legacy format loose object or
		// a new style loose object. The legacy format was completely
		// compressed with zlib so the first byte must be 0x78 (15-bit
		// window size, deflated) and the first 16 bit word must be
		// evenly divisible by 31. Otherwise its a new style loose
		// object.
		//
		final int fb = compressed[0] & 0xff;
		if (fb == 0x78 && (((fb << 8) | compressed[1] & 0xff) % 31) == 0) {
			final Inflater inflater = new Inflater(false);
			inflater.setInput(compressed);
			final byte[] hdr = new byte[64];
			int avail = 0;
			while (!inflater.finished() && avail < hdr.length)
				try {
					avail += inflater.inflate(hdr, avail, hdr.length - avail);
				} catch (DataFormatException dfe) {
					final CorruptObjectException coe;
					coe = new CorruptObjectException(getId(), "bad stream");
					coe.initCause(dfe);
					inflater.end();
					throw coe;
				}
			if (avail < 5)
				throw new CorruptObjectException(id, "no header");

			int pos;
			switch (hdr[0]) {
			case 'b':
				if (hdr[1] != 'l' || hdr[2] != 'o' || hdr[3] != 'b'
						|| hdr[4] != ' ')
					throw new CorruptObjectException(id, "invalid type");
				objectType = Constants.TYPE_BLOB;
				pos = 5;
				break;
			case 'c':
				if (avail < 7 || hdr[1] != 'o' || hdr[2] != 'm'
						|| hdr[3] != 'm' || hdr[4] != 'i' || hdr[5] != 't'
						|| hdr[6] != ' ')
					throw new CorruptObjectException(id, "invalid type");
				objectType = Constants.TYPE_COMMIT;
				pos = 7;
				break;
			case 't':
				switch (hdr[1]) {
				case 'a':
					if (hdr[2] != 'g' || hdr[3] != ' ')
						throw new CorruptObjectException(id, "invalid type");
					objectType = Constants.TYPE_TAG;
					pos = 4;
					break;
				case 'r':
					if (hdr[2] != 'e' || hdr[3] != 'e' || hdr[4] != ' ')
						throw new CorruptObjectException(id, "invalid type");
					objectType = Constants.TYPE_TREE;
					pos = 5;
					break;
				default:
					throw new CorruptObjectException(id, "invalid type");
				}
				break;
			default:
				throw new CorruptObjectException(id, "invalid type");
			}

			int tempSize = 0;
			while (pos < avail) {
				final int c = hdr[pos++];
				if (0 == c)
					break;
				else if (c < '0' || c > '9')
					throw new CorruptObjectException(id, "invalid length");
				tempSize *= 10;
				tempSize += c - '0';
			}
			objectSize = tempSize;
			bytes = new byte[objectSize];
			if (pos < avail)
				System.arraycopy(hdr, pos, bytes, 0, avail - pos);
			decompress(inflater, avail - pos);
		} else {
			int p = 0;
			int c = compressed[p++] & 0xff;
			final int typeCode = (c >> 4) & 7;
			int size = c & 15;
			int shift = 4;
			while ((c & 0x80) != 0) {
				c = compressed[p++] & 0xff;
				size += (c & 0x7f) << shift;
				shift += 7;
			}

			switch (typeCode) {
			case Constants.OBJ_COMMIT:
				objectType = Constants.TYPE_COMMIT;
				break;
			case Constants.OBJ_TREE:
				objectType = Constants.TYPE_TREE;
				break;
			case Constants.OBJ_BLOB:
				objectType = Constants.TYPE_BLOB;
				break;
			case Constants.OBJ_TAG:
				objectType = Constants.TYPE_TAG;
				break;
			default:
				throw new CorruptObjectException(id, "invalid type");
			}

			objectSize = size;
			bytes = new byte[objectSize];
			final Inflater inflater = new Inflater(false);
			inflater.setInput(compressed, p, compressed.length - p);
			decompress(inflater, 0);
		}
	}

	private void decompress(final Inflater inf, int p) throws IOException {
		try {
			while (!inf.finished())
				p += inf.inflate(bytes, p, objectSize - p);
		} catch (DataFormatException dfe) {
			final CorruptObjectException coe;
			coe = new CorruptObjectException(getId(), "bad stream");
			coe.initCause(dfe);
			throw coe;
		} finally {
			inf.end();
		}
		if (p != objectSize)
			new CorruptObjectException(getId(), "incorrect length");
	}

	public String getType() {
		return objectType;
	}

	public long getSize() {
		return objectSize;
	}

	public byte[] getBytes() {
		return bytes;
	}
}
