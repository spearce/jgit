package org.spearce.jgit.errors;

import java.io.IOException;

public class ObjectWritingException extends IOException
{
    private static final long serialVersionUID = 1L;

    public ObjectWritingException(final String s)
    {
        super(s);
    }
}
