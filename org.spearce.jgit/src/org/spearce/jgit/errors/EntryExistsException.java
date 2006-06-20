package org.spearce.jgit.errors;

import java.io.IOException;

public class EntryExistsException extends IOException
{
    private static final long serialVersionUID = 1L;

    public EntryExistsException(final String name)
    {
        super("Tree entry \"" + name + "\" already exists.");
    }
}
