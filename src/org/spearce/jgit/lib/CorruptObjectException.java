package org.spearce.jgit.lib;

import java.io.IOException;

public class CorruptObjectException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptObjectException(final String message) {
        super(message);
    }
}
