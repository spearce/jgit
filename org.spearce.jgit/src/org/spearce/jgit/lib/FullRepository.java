package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class FullRepository implements Repository {
    private static final String[] refSearchPaths = { "", "refs/", "refs/tags/",
            "refs/heads/", };

    private final File gitDir;

    private final File objectsDir;

    private final File refsDir;

    public FullRepository(final File d) {
        gitDir = d.getAbsoluteFile();
        objectsDir = new File(gitDir, "objects");
        refsDir = new File(gitDir, "refs");
    }

    public void create() throws IOException {
        final FileWriter cfg;

        if (gitDir.exists()) {
            throw new IllegalStateException("Repository already exists: "
                    + gitDir);
        }

        gitDir.mkdirs();

        objectsDir.mkdirs();
        new File(objectsDir, "pack").mkdir();
        new File(objectsDir, "info").mkdir();

        refsDir.mkdir();
        new File(refsDir, "heads").mkdir();
        new File(refsDir, "tags").mkdir();

        new File(gitDir, "branches").mkdir();
        new File(gitDir, "remotes").mkdir();
        writeSymref("HEAD", "refs/heads/master");

        // TODO: Implement a real config file reader/writer
        cfg = new FileWriter(new File(gitDir, "config"));
        try {
            cfg.write("[core]\n");
            cfg.write("\trepositoryformatversion = 0\n");
            cfg.write("\tfilemode = true\n");
        } finally {
            cfg.close();
        }
    }

    public File getDirectory() {
        return gitDir;
    }

    public File getObjectsDirectory() {
        return objectsDir;
    }

    public File toFile(final ObjectId objectId) {
        final String n = objectId.toString();
        return new File(new File(objectsDir, n.substring(0, 2)), n.substring(2));
    }

    public boolean hasObject(final ObjectId objectId) {
        return toFile(objectId).isFile();
    }

    public ObjectReader openBlob(final ObjectId id) throws IOException {
        final InputStream fis = openObjectStream(id);
        if (fis == null) {
            return null;
        }

        try {
            final ObjectReader or = new UnpackedObjectReader(id, fis);
            if (Constants.TYPE_BLOB.equals(or.getType())) {
                return or;
            } else {
                throw new CorruptObjectException(id, "not a blob");
            }
        } catch (IOException ioe) {
            fis.close();
            throw ioe;
        }
    }

    public ObjectReader openTree(final ObjectId id) throws IOException {
        final InputStream fis = openObjectStream(id);
        if (fis == null) {
            return null;
        }

        try {
            final ObjectReader or = new UnpackedObjectReader(id, fis);
            if (Constants.TYPE_TREE.equals(or.getType())) {
                return or;
            } else {
                throw new CorruptObjectException(id, "not a tree");
            }
        } catch (IOException ioe) {
            fis.close();
            throw ioe;
        }
    }

    public Commit mapCommit(final ObjectId id) throws IOException {
        final InputStream fis = openObjectStream(id);
        if (fis == null) {
            return null;
        }

        try {
            final ObjectReader or = new UnpackedObjectReader(id, fis);
            try {
                if (Constants.TYPE_COMMIT.equals(or.getType())) {
                    return new Commit(this, id, or.getBufferedReader());
                } else {
                    throw new CorruptObjectException(id, "not a commit");
                }
            } finally {
                or.close();
            }
        } catch (IOException ioe) {
            fis.close();
            throw ioe;
        }
    }

    public Tree mapTree(final ObjectId id) throws IOException {
        final InputStream fis = openObjectStream(id);
        if (fis == null) {
            return null;
        }

        try {
            final ObjectReader or = new UnpackedObjectReader(id, fis);
            try {
                if (Constants.TYPE_TREE.equals(or.getType())) {
                    return new Tree(this, id, or.getInputStream());
                } else if (Constants.TYPE_COMMIT.equals(or.getType())) {
                    return new Commit(this, id, or.getBufferedReader())
                            .getTree();
                } else {
                    throw new CorruptObjectException(id, "not a tree-ish");
                }
            } finally {
                or.close();
            }
        } catch (IOException ioe) {
            fis.close();
            throw ioe;
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

    private InputStream openObjectStream(final ObjectId objectId)
            throws IOException {
        try {
            return new FileInputStream(toFile(objectId));
        } catch (FileNotFoundException fnfe) {
            return null;
        }
    }

    private void writeSymref(final String name, final String target)
            throws IOException {
        final File s = new File(gitDir, name);
        final File t = File.createTempFile("srf", null, gitDir);
        FileWriter w = new FileWriter(t);
        try {
            w.write("ref: ");
            w.write(target);
            w.write('\n');
            w.close();
            w = null;
            if (!t.renameTo(s)) {
                s.getParentFile().mkdirs();
                if (!t.renameTo(s)) {
                    t.delete();
                    throw new WritingNotSupportedException("Unable to"
                            + " write symref " + name + " to point to "
                            + target);
                }
            }
        } finally {
            if (w != null) {
                w.close();
                t.delete();
            }
        }
    }

    private ObjectId readRef(final String name) throws IOException {
        final File f = new File(gitDir, name);
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

    public String toString() {
        return "FullRepository[" + getDirectory() + "]";
    }
}
