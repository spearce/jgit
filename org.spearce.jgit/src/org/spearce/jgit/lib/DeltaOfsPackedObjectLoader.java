package org.spearce.jgit.lib;

import java.io.IOException;

/** Reads a deltified object which uses an offset to find its base. */
class DeltaOfsPackedObjectLoader extends DeltaPackedObjectLoader {
	private final long deltaBase;

	DeltaOfsPackedObjectLoader(final WindowCursor curs,
			final PackFile pr, final long offset,
			final int deltaSz, final long base) {
		super(curs, pr, offset, deltaSz);
		deltaBase = base;
	}

	protected PackedObjectLoader getBaseLoader() throws IOException {
		return pack.resolveBase(curs, deltaBase);
	}
}
