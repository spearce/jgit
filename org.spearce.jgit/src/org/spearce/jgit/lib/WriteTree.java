package org.spearce.jgit.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

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

    private final Repository r;

    private final ObjectWriter ow;

    private boolean writeAll;

    private File src;

    public WriteTree(final Repository db, final File sourceDir) {
        r = db;
        ow = new ObjectWriter(r);
        writeAll = false;
        src = sourceDir;
    }

    public void setWriteAll(final boolean t) {
        writeAll = t;
    }

    protected void visitFile(final FileTreeEntry f) throws IOException {
        super.visitFile(f);
        if (f.isModified()) {
            f.setId(ow.writeBlob(new File(src, f.getName())));
        }
    }

    protected void visitSymlink(final SymlinkTreeEntry s) throws IOException {
        super.visitSymlink(s);
        if (!r.hasObject(s.getId())) {
            throw new MissingObjectException("blob", s.getId());
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
                    throw new WritingNotSupportedException("Object type not"
                            + " supported as member of Tree:" + e);
                }

                o.write(mode);
                o.write(' ');
                o.write(e.getNameUTF8());
                o.write(0);
                o.write(e.getId().getBytes());
            }
            t.setId(ow.writeTree(o.toByteArray()));
        }
    }
}
