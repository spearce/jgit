package org.spearce.jgit.lib;

import java.io.IOException;

public class CorruptObjectException extends IOException
{
    private static final long serialVersionUID = 1L;

    public CorruptObjectException(final ObjectId id, final String why)
    {
        super("Object " + id + " is corrupt: " + why);
    }

    public CorruptObjectException(final String why)
    {
        super(why);
    }
}
