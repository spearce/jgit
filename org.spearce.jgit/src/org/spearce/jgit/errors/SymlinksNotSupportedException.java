package org.spearce.jgit.errors;

import java.io.IOException;

public class SymlinksNotSupportedException extends IOException
{
    private static final long serialVersionUID = 1L;

    public SymlinksNotSupportedException(final String s)
    {
        super(s);
    }
}
