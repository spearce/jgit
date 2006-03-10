package org.spearce.jgit.lib;

import java.io.IOException;

public class MissingObjectException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingObjectException(final String type, final ObjectId id) {
        super("Missing " + type + " " + id);
    }
}
