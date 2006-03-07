package org.spearce.jgit.lib;

import java.io.UnsupportedEncodingException;

public class SymlinkTreeEntry extends TreeEntry {
    public SymlinkTreeEntry(final Tree parent, final ObjectId id,
            final byte[] nameUTF8) throws UnsupportedEncodingException {
        super(parent, id, nameUTF8);
    }

    public String toString() {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(" S ");
        r.append(getFullName());
        return r.toString();
    }
}
