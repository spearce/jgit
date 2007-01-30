package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.zip.DataFormatException;

import org.spearce.jgit.errors.CorruptObjectException;

/** Reader for a deltafied object stored in a pack file. */
abstract class DeltaPackedObjectLoader extends PackedObjectLoader {
	private final int deltaSize;

	DeltaPackedObjectLoader(final PackFile pr, final long offset,
			final int deltaSz) {
		super(pr, offset);
		deltaSize = deltaSz;
	}

	public String getType() throws IOException {
		if (objectType == null)
			getBytes();
		return objectType;
	}

	public long getSize() throws IOException {
		if (objectType == null)
			getBytes();
		return objectSize;
	}

	public byte[] getBytes() throws IOException {
		try {
			final ObjectLoader baseLoader = getBaseLoader();
			final byte[] data = BinaryDelta.apply(baseLoader.getBytes(), pack
					.decompress(dataOffset, deltaSize));
			objectType = baseLoader.getType();
			objectSize = data.length;
			return data;
		} catch (DataFormatException dfe) {
			final CorruptObjectException coe;
			coe = new CorruptObjectException(getId(), "bad stream");
			coe.initCause(dfe);
			throw coe;
		}
	}

	protected abstract ObjectLoader getBaseLoader() throws IOException;
}
