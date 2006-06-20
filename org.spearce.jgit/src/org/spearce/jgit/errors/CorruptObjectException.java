package org.spearce.jgit.errors;

import java.io.IOException;

import org.spearce.jgit.lib.ObjectId;

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
