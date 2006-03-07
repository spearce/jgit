package org.spearce.jgit.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;

public class WriteTree extends TreeVisitor {
    private static final TreeNameComparator TNC = new TreeNameComparator();

    private static final byte[] TREE_MODE;

    private static final byte[] SYMLINK_MODE;

    private static final byte[] PLAIN_FILE_MODE;

    private static final byte[] EXECUTABLE_FILE_MODE;

    static {
        try {
            TREE_MODE = "40000".getBytes("UTF-8");
            SYMLINK_MODE = "120000".getBytes("UTF-8");
            PLAIN_FILE_MODE = "100644".getBytes("UTF-8");
            EXECUTABLE_FILE_MODE = "100755".getBytes("UTF-8");
        } catch (UnsupportedEncodingException uue) {
            throw new ExceptionInInitializerError(uue);
        }
    }

    private final ObjectDatabase objdb;

    private final byte[] copybuf;

    private final MessageDigest md;

    private final boolean writeAll;

    private File src;

    public WriteTree(final ObjectDatabase db, final File sourceDir)
            throws NoSuchAlgorithmException {
        objdb = db;
        copybuf = new byte[8192];
        md = MessageDigest.getInstance("SHA-1");
        writeAll = true;
        src = sourceDir;
    }

    protected void visitFile(final FileTreeEntry f) throws IOException {
        super.visitFile(f);
        final File d = new File(src, f.getName());
        final FileInputStream is = new FileInputStream(d);
        try {
            f.setId(writeObject("blob", (int) d.length(), is));
        } finally {
            is.close();
        }
    }

    protected void visitSymlink(final SymlinkTreeEntry s) throws IOException {
        super.visitSymlink(s);

        if (!objdb.checkObject(s.getId())) {
            throw new CorruptObjectException("Missing symlink blob "
                    + s.getId());
        }
    }

    protected void visitTree(final Tree t) throws IOException {
        // Only visit a tree if it has been modified. If any child of a tree has
        // been modified then the tree itself will also appear modified;
        // consequently we will recurse into it.
        //
        if (writeAll || t.isModified()) {
            if (t.isRoot()) {
                super.visitTree(t);
            } else {
                final File d = new File(src, t.getName());
                final File o = src;
                src = d;
                super.visitTree(t);
                src = o;
            }

            final ByteArrayOutputStream o = new ByteArrayOutputStream();
            final ArrayList r = new ArrayList();
            final byte[] d;
            Iterator i;

            i = t.entryIterator();
            while (i.hasNext()) {
                r.add(i.next());
            }
            Collections.sort(r, TNC);

            i = r.iterator();
            while (i.hasNext()) {
                final TreeEntry e = (TreeEntry) i.next();
                final byte[] mode;

                if (e instanceof Tree) {
                    mode = TREE_MODE;
                } else if (e instanceof SymlinkTreeEntry) {
                    mode = SYMLINK_MODE;
                } else if (e instanceof FileTreeEntry) {
                    mode = ((FileTreeEntry) e).isExecutable() ? EXECUTABLE_FILE_MODE
                            : PLAIN_FILE_MODE;
                } else {
                    throw new IOException("Object not supported in Tree:" + e);
                }

                o.write(mode);
                o.write(' ');
                o.write(e.getNameUTF8());
                o.write(0);
                o.write(e.getId().getBytes());
            }
            d = o.toByteArray();
            t.setId(writeObject("tree", d.length, new ByteArrayInputStream(d)));
        }
    }

    private ObjectId writeObject(final String type, int len,
            final InputStream is) throws IOException {
        final File t = File.createTempFile("noz", null, objdb
                .getObjectsDirectory());
        final DeflaterOutputStream ts = new DeflaterOutputStream(
                new FileOutputStream(t));
        ObjectId id = null;
        try {
            final byte[] header = (type + " " + len + "\0").getBytes("UTF-8");
            int r;

            md.update(header);
            ts.write(header);

            while ((r = is.read(copybuf)) > 0 && len > 0) {
                if (r > len) {
                    r = len;
                }
                md.update(copybuf, 0, r);
                ts.write(copybuf, 0, r);
                len -= r;
            }
            if (len != 0) {
                throw new CorruptObjectException("Input was short: " + len);
            }

            ts.close();
            id = new ObjectId(md.digest());
        } finally {
            if (id == null) {
                md.reset();
                ts.close();
                t.delete();
            }
        }

        if (objdb.checkObject(id)) {
            // Object is already in the repository so remove the temporary file.
            //
            t.delete();
        } else {
            final File o = objdb.objectFile(id);
            o.getParentFile().mkdir();
            if (!t.renameTo(o)) {
                if (!objdb.checkObject(id)) {
                    // The object failed to be renamed into its proper location
                    // and it doesn't exist in the repository either. (The
                    // rename could have failed if another process was placing
                    // the object there at the same time as us.) We really don't
                    // know what went wrong, so abort.
                    //
                    t.delete();
                    throw new IOException("Failed to write object " + id);
                }
            }
        }

        return id;
    }
}
