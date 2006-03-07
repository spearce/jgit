package org.spearce.jgit.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class Tree extends TreeEntry implements Treeish {
    private final ObjectDatabase objdb;

    private Collection allEntries;

    private Map entriesByName;

    public Tree(final ObjectDatabase db, final ObjectId myId,
            final InputStream is) throws IOException {
        this(db, null, myId, null);
        readTree(is);
    }

    public Tree(final ObjectDatabase db, final Tree parent,
            final ObjectId myId, final byte[] nameUTF8)
            throws UnsupportedEncodingException {
        super(parent, myId, nameUTF8);
        objdb = db;
    }

    public boolean isModified() {
        return getId() == null;
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    public ObjectDatabase getDatabase() {
        return objdb;
    }

    public final ObjectId getTreeId() {
        return getId();
    }

    public final Tree getTree() {
        return this;
    }

    public boolean isLoaded() {
        return entriesByName != null;
    }

    public Iterator entryIterator() throws IOException {
        if (!isLoaded()) {
            final ObjectReader or = objdb.openTree(getId());
            try {
                readTree(or.getInputStream());
            } finally {
                or.close();
            }
        }
        return allEntries.iterator();
    }

    public String toString() {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(" T ");
        r.append(getFullName());
        return r.toString();
    }

    private void readTree(final InputStream is) throws IOException {
        final Map tempEnts = new TreeMap();
        for (;;) {
            int c;
            int mode;
            final ByteArrayOutputStream nameBuf;
            final byte[] entId;
            final byte[] name;
            final ObjectId id;
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
            name = nameBuf.toByteArray();
            if ((mode & 040000) != 0) {
                ent = new Tree(objdb, this, id, name);
            } else if ((mode & 020000) != 0) {
                ent = new SymlinkTreeEntry(this, id, name);
            } else {
                ent = new FileTreeEntry(this, id, name, (mode & 0100) != 0);
            }
            tempEnts.put(ent.getName(), ent);
        }

        entriesByName = tempEnts;
        allEntries = Collections.unmodifiableCollection(entriesByName.values());
    }
}
