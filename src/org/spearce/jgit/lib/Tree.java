package org.spearce.jgit.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Tree implements Treeish {
    private final ObjectDatabase objdb;

    private final ObjectId treeId;

    private final List entries;

    public Tree(final ObjectDatabase db, final ObjectId id, final InputStream is)
            throws IOException {
        objdb = db;
        treeId = id;

        final ArrayList tempEnts = new ArrayList();
        for (;;) {
            int c;
            int mode;
            final ByteArrayOutputStream nameBuf;
            final byte[] entId;
            int entIdLen;

            c = is.read();
            if (c == -1) {
                break;
            }
            if (c < '0' || c > '7') {
                throw new CorruptObjectException("Invalid tree entry in " + id);
            }
            mode = c - '0';
            for (;;) {
                c = is.read();
                if (' ' == c) {
                    break;
                }
                if (c < '0' || c > '7') {
                    throw new CorruptObjectException("Invalid tree entry in "
                            + id);
                }
                mode *= 8;
                mode += c - '0';
            }

            nameBuf = new ByteArrayOutputStream(128);
            for (;;) {
                c = is.read();
                if (c == -1) {
                    throw new CorruptObjectException("Invalid tree entry in "
                            + id);
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
                throw new CorruptObjectException("Invalid tree entry in " + id);
            }

            tempEnts.add(new Entry(mode, new String(nameBuf.toByteArray(),
                    "UTF-8"), new ObjectId(entId)));
        }

        entries = tempEnts;
    }

    public ObjectId getTreeId() {
        return treeId;
    }

    public List getTreeEntries() {
        return entries;
    }

    public class Entry {
        private final int mode;

        private final String name;

        private final ObjectId id;

        private Tree treeObj;

        private Entry(final int mode, final String name, final ObjectId id) {
            this.mode = mode;
            this.name = name;
            this.id = id;
        }

        public boolean isTree() {
            return (getMode() & 040000) != 0;
        }

        public boolean isSymlink() {
            return (getMode() & 020000) != 0;
        }

        public boolean isExecutable() {
            return (getMode() & 0100) != 0;
        }

        public String getName() {
            return name;
        }

        public int getMode() {
            return mode;
        }

        public ObjectId getId() {
            return id;
        }

        public Tree getTree() throws IOException {
            if (treeObj == null) {
                treeObj = objdb.openTree(getId());
            }
            return treeObj;
        }

        public ObjectReader openBlob() throws IOException {
            return objdb.openBlob(getId());
        }

        public String toString() {
            final StringBuffer r = new StringBuffer();
            final String modeStr = Integer.toString(getMode(), 8);
            r.append(getId());
            r.append(' ');
            if (isTree()) {
                r.append('D');
            } else if (isSymlink()) {
                r.append('S');
            } else if (isExecutable()) {
                r.append('X');
            } else {
                r.append('F');
            }
            r.append(' ');
            if (modeStr.length() == 5) {
                r.append('0');
            }
            r.append(modeStr);
            r.append(' ');
            r.append(getName());
            return r.toString();
        }
    }
}
