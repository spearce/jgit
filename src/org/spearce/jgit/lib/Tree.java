package org.spearce.jgit.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Tree extends TreeEntry implements Treeish {
    private final ObjectDatabase objdb;

    private List entries;

    public Tree(final ObjectDatabase db, final ObjectId myId,
            final InputStream is) throws IOException {
        this(db, null, myId, null);
        readTree(is);
    }

    public Tree(final ObjectDatabase db, final Tree parent,
            final ObjectId myId, final String name) {
        super(parent, myId, name);
        objdb = db;
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    public ObjectDatabase getDatabase() {
        return objdb;
    }

    public ObjectId getTreeId() {
        return getId();
    }

    public List getTreeEntries() throws IOException {
        if (entries == null) {
            final ObjectReader or = objdb.openTree(getId());
            try {
                readTree(or.getInputStream());
            } finally {
                or.close();
            }
        }
        return entries;
    }

    public String toString() {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(" T ");
        r.append(getFullName());
        return r.toString();
    }

    private void readTree(final InputStream is) throws IOException {
        final ArrayList tempEnts = new ArrayList();
        for (;;) {
            int c;
            int mode;
            final ByteArrayOutputStream nameBuf;
            final byte[] entId;
            final ObjectId id;
            final String name;
            final TreeEntry ent;
            int entIdLen;

            c = is.read();
            if (c == -1) {
                break;
            }
            if (c < '0' || c > '7') {
                throw new CorruptObjectException("Invalid tree entry in "
                        + getId());
            }
            mode = c - '0';
            for (;;) {
                c = is.read();
                if (' ' == c) {
                    break;
                }
                if (c < '0' || c > '7') {
                    throw new CorruptObjectException("Invalid tree entry in "
                            + getId());
                }
                mode *= 8;
                mode += c - '0';
            }

            nameBuf = new ByteArrayOutputStream(128);
            for (;;) {
                c = is.read();
                if (c == -1) {
                    throw new CorruptObjectException("Invalid tree entry in "
                            + getId());
                }
                if (0 == c) {
                    break;
                }
                nameBuf.write(c);
            }

            entId = new byte[20];
            entIdLen = 0;
            while ((c = is.read(entId, entIdLen, entId.length - entIdLen)) > 0) {
                entIdLen += c;
            }
            if (entIdLen != entId.length) {
                throw new CorruptObjectException("Invalid tree entry in "
                        + getId());
            }

            id = new ObjectId(entId);
            name = new String(nameBuf.toByteArray(), "UTF-8");
            if ((mode & 040000) != 0) {
                ent = new Tree(objdb, this, id, name);
            } else if ((mode & 020000) != 0) {
                ent = new SymlinkTreeEntry(this, id, name);
            } else {
                ent = new FileTreeEntry(this, id, name, (mode & 0100) != 0);
            }
            tempEnts.add(ent);
        }

        entries = tempEnts;
    }
}
