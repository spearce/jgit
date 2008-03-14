package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.zip.DataFormatException;

import org.spearce.jgit.errors.CorruptObjectException;

/** Reader for a non-delta (just deflated) object in a pack file. */
class WholePackedObjectLoader extends PackedObjectLoader {
	private static final int OBJ_COMMIT = Constants.OBJ_COMMIT;

	WholePackedObjectLoader(final PackFile pr, final long offset,
			final int type, final int size) {
		super(pr, offset);
		objectType = type;
		objectSize = size;
	}

	@Override
	protected byte[] getCachedBytes() throws IOException {
		if (objectType != OBJ_COMMIT) {
			final UnpackedObjectCache.Entry cache = pack.readCache(dataOffset);
			if (cache != null)
				return cache.data;
		}

		try {
			final byte[] data = pack.decompress(dataOffset, objectSize);
			if (objectType != OBJ_COMMIT)
				pack.saveCache(dataOffset, data, objectType);
			return data;
		} catch (DataFormatException dfe) {
			final CorruptObjectException coe;
			coe = new CorruptObjectException(getId(), "bad stream");
			coe.initCause(dfe);
			throw coe;
		}
	}
}
