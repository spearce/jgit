package org.spearce.jgit.lib;

import java.io.IOException;

/** Reads a deltafied object which uses an offset to find its base. */
class DeltaOfsPackedObjectLoader extends DeltaPackedObjectLoader {
	private final long deltaBase;

	public DeltaOfsPackedObjectLoader(final PackFile pr, final long offset,
			final int deltaSz, final long base) {
		super(pr, offset, deltaSz);
		deltaBase = base;
	}

	protected ObjectLoader getBaseLoader() throws IOException {
		return pack.resolveBase(deltaBase);
	}
}
