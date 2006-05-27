package org.spearce.jgit.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class Tree extends TreeEntry implements Treeish {
    private final Repository r;

    private Collection allEntries;

    private Map entriesByName;

    public Tree(final Repository db) {
        this(db, null, null, null);
    }

    public Tree(final Repository db, final ObjectId myId, final InputStream is)
            throws IOException {
        this(db, null, myId, null);
        readTree(is);
    }

    public Tree(final Repository db, final Tree parent, final ObjectId myId,
            final byte[] nameUTF8) {
        super(parent, myId, nameUTF8);
        r = db;
        if (myId == null) {
            entriesByName = new TreeMap();
            allEntries = Collections.unmodifiableCollection(entriesByName
                    .values());
        }
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    public Repository getDatabase() {
        return r;
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

    public FileTreeEntry addFile(final String name, final ObjectId id)
            throws IOException, MissingObjectException {
        ensureLoaded();
        final FileTreeEntry n;
        n = new FileTreeEntry(this, id, name.getBytes("UTF-8"), false);
        entriesByName.put(n.getName(), n);
        setModified();
        return n;
    }

    public Tree addTree(final String name, final ObjectId id)
            throws IOException, MissingObjectException {
        ensureLoaded();
        final Tree n = new Tree(r, this, id, name.getBytes("UTF-8"));
        entriesByName.put(n.getName(), n);
        setModified();
        return n;
    }

    public Iterator entryIterator() throws IOException, MissingObjectException {
        ensureLoaded();
        return allEntries.iterator();
    }

    public TreeEntry findMember(String s) throws IOException,
            MissingObjectException {
        ensureLoaded();
        final int slash = s.indexOf('/');
        final String remainder;
        if (slash != -1) {
            remainder = s.substring(slash + 1);
            s = s.substring(0, slash);
        } else {
            remainder = null;
        }

        final TreeEntry e = (TreeEntry) entriesByName.get(s);
        if (e != null && remainder != null) {
            if (e instanceof Tree) {
                return ((Tree) e).findMember(remainder);
            } else {
                return null;
            }
        } else {
            return e;
        }
    }

    private void ensureLoaded() throws IOException, MissingObjectException {
        if (!isLoaded()) {
            final ObjectReader or = r.openTree(getId());
            if (or == null) {
                throw new MissingObjectException(Constants.TYPE_TREE, getId());
            }
            try {
                readTree(or.getInputStream());
            } finally {
                or.close();
            }
        }
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
                throw new CorruptObjectException(getId(), "invalid mode");
            }
            mode = c - '0';
            for (;;) {
                c = is.read();
                if (' ' == c) {
                    break;
                }
                if (c < '0' || c > '7') {
                    throw new CorruptObjectException(getId(), "invalid mode");
                }
                mode *= 8;
                mode += c - '0';
            }

            nameBuf = new ByteArrayOutputStream(128);
            for (;;) {
                c = is.read();
                if (c == -1) {
                    throw new CorruptObjectException(getId(), "unexpected eof");
                }
                if (0 == c) {
                    break;
                }
                nameBuf.write(c);
            }

            entId = new byte[Constants.OBJECT_ID_LENGTH];
            entIdLen = 0;
            while ((c = is.read(entId, entIdLen, entId.length - entIdLen)) > 0) {
                entIdLen += c;
            }
            if (entIdLen != entId.length) {
                throw new CorruptObjectException(getId(), "missing hash");
            }

            id = new ObjectId(entId);
            name = nameBuf.toByteArray();
            if ((mode & 040000) != 0) {
                ent = new Tree(r, this, id, name);
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

    public String toString() {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(" T ");
        r.append(getFullName());
        return r.toString();
    }
}