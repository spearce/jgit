package org.spearce.jgit.errors;

import java.io.IOException;

import org.spearce.jgit.lib.ObjectId;

public class MissingObjectException extends IOException
{
    private static final long serialVersionUID = 1L;

    public MissingObjectException(final ObjectId id, final String type)
    {
        super("Missing " + type + " " + id);
    }
}
