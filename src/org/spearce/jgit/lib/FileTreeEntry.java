package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class FileTreeEntry extends TreeEntry {
    private boolean executable;

    public FileTreeEntry(final Tree parent, final ObjectId id,
            final byte[] nameUTF8, final boolean execute)
            throws UnsupportedEncodingException {
        super(parent, id, nameUTF8);
        executable = execute;
    }

    public boolean isExecutable() {
        return executable;
    }

    public ObjectReader openBlob() throws IOException {
        return getParent().getDatabase().openBlob(getId());
    }

    public String toString() {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(' ');
        r.append(isExecutable() ? 'X' : 'F');
        r.append(' ');
        r.append(getFullName());
        return r.toString();
    }
}
