package org.spearce.jgit.lib;

import java.io.IOException;

import org.spearce.jgit.errors.MissingObjectException;

/** Reads a deltaified object which uses an {@link ObjectId} to find its base. */
class DeltaRefPackedObjectReader extends DeltaPackedObjectReader
{
    private final ObjectId deltaBase;

    public DeltaRefPackedObjectReader(
        final PackReader pr,
        final long offset,
        final ObjectId base)
    {
        super(pr, offset);
        deltaBase = base;
    }

    protected ObjectReader baseReader() throws IOException
    {
        final ObjectReader or = pack.resolveBase(deltaBase);
        if (or == null)
        {
            throw new MissingObjectException(deltaBase, "delta base");
        }
        return or;
    }
}
