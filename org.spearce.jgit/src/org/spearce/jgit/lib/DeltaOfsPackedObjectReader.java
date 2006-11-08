package org.spearce.jgit.lib;

import java.io.IOException;

/** Reads a deltafied object which uses an offset to find its base. */
public class DeltaOfsPackedObjectReader extends DeltaPackedObjectReader
{
    private final long deltaBase;

    public DeltaOfsPackedObjectReader(
        final PackReader pr,
        final long offset,
        final long base)
    {
        super(pr, offset);
        deltaBase = base;
    }

    protected ObjectReader baseReader() throws IOException
    {
        return pack.resolveBase(deltaBase);
    }
}
