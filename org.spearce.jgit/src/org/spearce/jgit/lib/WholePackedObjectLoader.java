package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.zip.DataFormatException;

import org.spearce.jgit.errors.CorruptObjectException;

/** Reader for a non-delta (just deflated) object in a pack file. */
class WholePackedObjectLoader extends PackedObjectLoader {
	WholePackedObjectLoader(final PackFile pr, final long offset,
			final String type, final int size) {
		super(pr, offset);
		objectType = type;
		objectSize = size;
	}

	public byte[] getBytes() throws IOException {
		try {
			return pack.decompress(dataOffset, objectSize);
		} catch (DataFormatException dfe) {
			final CorruptObjectException coe;
			coe = new CorruptObjectException(getId(), "bad stream");
			coe.initCause(dfe);
			throw coe;
		}
	}
}
