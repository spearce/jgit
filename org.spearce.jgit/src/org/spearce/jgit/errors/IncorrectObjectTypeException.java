package org.spearce.jgit.errors;

import java.io.IOException;

import org.spearce.jgit.lib.ObjectId;

public class IncorrectObjectTypeException extends IOException
{
    private static final long serialVersionUID = 1L;

    public IncorrectObjectTypeException(final ObjectId id, final String type)
    {
        super("Object " + id + " is not a " + type + ".");
    }
}
