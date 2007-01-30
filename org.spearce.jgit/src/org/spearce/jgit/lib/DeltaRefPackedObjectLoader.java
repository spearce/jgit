package org.spearce.jgit.lib;

import java.io.IOException;

import org.spearce.jgit.errors.MissingObjectException;

/** Reads a deltaified object which uses an {@link ObjectId} to find its base. */
class DeltaRefPackedObjectLoader extends DeltaPackedObjectLoader {
	private final ObjectId deltaBase;

	DeltaRefPackedObjectLoader(final PackFile pr, final long offset,
			final int deltaSz, final ObjectId base) {
		super(pr, offset, deltaSz);
		deltaBase = base;
	}

	protected ObjectLoader getBaseLoader() throws IOException {
		final ObjectLoader or = pack.get(deltaBase,
				new byte[Constants.OBJECT_ID_LENGTH]);
		if (or == null)
			throw new MissingObjectException(deltaBase, "delta base");
		return or;
	}
}
