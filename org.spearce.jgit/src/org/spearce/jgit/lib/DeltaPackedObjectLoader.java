package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.zip.DataFormatException;

import org.spearce.jgit.errors.CorruptObjectException;

/** Reader for a deltified object stored in a pack file. */
abstract class DeltaPackedObjectLoader extends PackedObjectLoader {
	private static final int OBJ_COMMIT = Constants.OBJ_COMMIT;

	private final int deltaSize;

	DeltaPackedObjectLoader(final WindowCursor curs, final PackFile pr,
			final long offset, final int deltaSz) {
		super(curs, pr, offset);
		objectType = -1;
		deltaSize = deltaSz;
	}

	public int getType() throws IOException {
		if (objectType < 0)
			getCachedBytes();
		return objectType;
	}

	public long getSize() throws IOException {
		if (objectType < 0)
			getCachedBytes();
		return objectSize;
	}

	@Override
	public byte[] getCachedBytes() throws IOException {
		if (objectType != OBJ_COMMIT) {
			final UnpackedObjectCache.Entry cache = pack.readCache(dataOffset);
			if (cache != null) {
				curs.release();
				objectType = cache.type;
				objectSize = cache.data.length;
				return cache.data;
			}
		}

		try {
			final PackedObjectLoader baseLoader = getBaseLoader();
			final byte[] data = BinaryDelta.apply(baseLoader.getCachedBytes(),
					pack.decompress(dataOffset, deltaSize, curs));
			curs.release();
			objectType = baseLoader.getType();
			objectSize = data.length;
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

	/**
	 * @return the object loader for the base object
	 * @throws IOException
	 */
	protected abstract PackedObjectLoader getBaseLoader() throws IOException;
}
