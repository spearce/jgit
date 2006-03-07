package org.spearce.jgit.lib;

public abstract class TreeEntry {
    private final Tree parent;

    private ObjectId id;

    private final String name;

    public TreeEntry(final Tree parent, final ObjectId id, final String name) {
        this.parent = parent;
        this.id = id;
        this.name = name;
    }

    public Tree getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId n) {
        id = n;
    }

    public String getFullName() {
        final StringBuffer r = new StringBuffer();
        appendFullName(r);
        return r.toString();
    }

    private void appendFullName(final StringBuffer r) {
        final TreeEntry p = getParent();
        final String n = getName();
        if (p != null) {
            p.appendFullName(r);
            if (r.length() > 0) {
                r.append('/');
            }
        }
        if (n != null) {
            r.append(n);
        }
    }
}
