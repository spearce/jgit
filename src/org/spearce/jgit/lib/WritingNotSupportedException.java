package org.spearce.jgit.lib;

public class WritingNotSupportedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WritingNotSupportedException(final String why) {
        super(why);
    }

    public WritingNotSupportedException(final String why, final Throwable cause) {
        super(why, cause);
    }
}
