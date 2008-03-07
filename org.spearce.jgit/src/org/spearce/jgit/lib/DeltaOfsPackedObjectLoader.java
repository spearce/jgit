package org.spearce.jgit.lib;

import java.io.IOException;

/** Reads a deltified object which uses an offset to find its base. */
class DeltaOfsPackedObjectLoader extends DeltaPackedObjectLoader {
	private final long deltaBase;

	/**
	 * Constructor
	 *
	 * @param pr
	 *            The packfile holding the delta packed object to load
	 * @param offset
	 *            offset relative to base
	 * @param deltaSz
	 *            size of delta
	 * @param base
	 *            offset of the delta packed object
	 */
	public DeltaOfsPackedObjectLoader(final PackFile pr, final long offset,
			final int deltaSz, final long base) {
		super(pr, offset, deltaSz);
		deltaBase = base;
	}

	protected PackedObjectLoader getBaseLoader() throws IOException {
		return pack.resolveBase(deltaBase);
	}
}
