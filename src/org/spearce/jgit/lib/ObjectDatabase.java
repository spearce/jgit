package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class ObjectDatabase {
    private static final String[] refSearchPaths = { "", "refs/", "refs/tags/",
            "refs/heads/", };

    private final File db;

    private final File objectsDir;

    public ObjectDatabase(final File d) {
        db = d.getAbsoluteFile();
        objectsDir = new File(db, "objects");
    }

    private InputStream openObjectStream(final ObjectId objectId)
            throws IOException {
        try {
            final String n = objectId.toString();
            return new FileInputStream(new File(new File(objectsDir, n
                    .substring(0, 2)), n.substring(2)));
        } catch (FileNotFoundException fnfe) {
            return null;
        }
    }

    public ObjectReader openBlob(final ObjectId id) throws IOException {
        final InputStream fis = openObjectStream(id);
        if (fis == null) {
            return null;
        }

        try {
            final ObjectReader or = new ObjectReader(id, fis);
            if ("blob".equals(or.getType())) {
                return or;
            } else {
                throw new CorruptObjectException("Not a blob " + id);
            }
        } catch (IOException ioe) {
            fis.close();
            throw ioe;
        }
    }

    public Commit openCommit(final ObjectId id) throws IOException {
        final InputStream fis = openObjectStream(id);
        if (fis == null) {
            return null;
        }

        try {
            final ObjectReader or = new ObjectReader(id, fis);
            try {
                if ("commit".equals(or.getType())) {
                    return new Commit(this, id, or.getBufferedReader());
                } else {
                    throw new CorruptObjectException("Not a commit: " + id);
                }
            } finally {
                or.close();
            }
        } catch (IOException ioe) {
            fis.close();
            throw ioe;
        }
    }

    public Tree openTree(final ObjectId id) throws IOException {
        final InputStream fis = openObjectStream(id);
        if (fis == null) {
            return null;
        }

        try {
            final ObjectReader or = new ObjectReader(id, fis);
            try {
                if ("commit".equals(or.getType())) {
                    return openTree(new Commit(this, id, or.getBufferedReader())
                            .getTreeId());
                } else if ("tree".equals(or.getType())) {
                    return new Tree(this, id, or.getInputStream());
                } else {
                    throw new CorruptObjectException("Not a tree-ish: " + id);
                }
            } finally {
                or.close();
            }
        } catch (IOException ioe) {
            fis.close();
            throw ioe;
        }
    }

    private ObjectId readRef(final String name) throws IOException {
        final File f = new File(db, name);
        if (!f.isFile()) {
            return null;
        }
        final BufferedReader fr = new BufferedReader(new FileReader(f));
        try {
            final String line = fr.readLine();
            if (line == null || line.length() == 0) {
                return null;
            }
            if (line.startsWith("ref: ")) {
                return readRef(line.substring("ref: ".length()));
            }
            if (ObjectId.isId(line)) {
                return new ObjectId(line);
            }
            throw new IOException("Not a ref: " + name + ": " + line);
        } finally {
            fr.close();
        }
    }

    public ObjectId resolveRevision(final String r) throws IOException {
        ObjectId id = null;

        if (ObjectId.isId(r)) {
            id = new ObjectId(r);
        }
        if (id == null) {
            for (int k = 0; k < refSearchPaths.length; k++) {
                id = readRef(refSearchPaths[k] + r);
                if (id != null) {
                    break;
                }
            }
        }
        return id;
    }

    public String toString() {
        return "ObjectDatabase[" + db + "]";
    }
}
