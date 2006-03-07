package org.spearce.jgit.lib;

public class SymlinkTreeEntry extends TreeEntry {
    public SymlinkTreeEntry(final Tree parent, final ObjectId id,
            final String name) {
        super(parent, id, name);
    }

    public String toString() {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(" S ");
        r.append(getFullName());
        return r.toString();
    }
}
