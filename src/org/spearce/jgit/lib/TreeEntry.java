package org.spearce.jgit.lib;

import java.io.UnsupportedEncodingException;

public abstract class TreeEntry {
    private final Tree parent;

    private ObjectId id;

    private final byte[] nameUTF8;

    private final String name;

    protected TreeEntry(final Tree myParent, final ObjectId myId,
            final byte[] myNameUTF8) throws UnsupportedEncodingException {
        parent = myParent;
        id = myId;
        nameUTF8 = myNameUTF8;
        name = nameUTF8 != null ? new String(nameUTF8, "UTF-8") : null;
    }

    public Tree getParent() {
        return parent;
    }

    public byte[] getNameUTF8() {
        return nameUTF8;
    }

    public String getName() {
        return name;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId n) {
        // If we have a parent and our id is being cleared or changed then force
        // the parent's id to become unset as it depends on our id.
        //
        final Tree p = getParent();
        if (p != null) {
            if ((id == null && n != null) || (id != null && n == null)
                    || !id.equals(n)) {
                p.setId(null);
            }
        }

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
