package org.spearce.jgit.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CopyTreeToDirectory extends TreeVisitor {
    private final byte[] copyBuffer = new byte[8192];

    private File dest;

    public CopyTreeToDirectory(final File x) {
        dest = x;
    }

    protected void visitTree(final Tree t) throws IOException {
        if (t.isRoot()) {
            dest.mkdirs();
            super.visitTree(t);
        } else {
            final File d = new File(dest, t.getName());
            final File o = dest;
            d.mkdir();
            dest = d;
            super.visitTree(t);
            dest = o;
        }
    }

    protected void visitFile(final FileTreeEntry f) throws IOException {
        final File d = new File(dest, f.getName());
        final ObjectReader or = f.openBlob();
        final InputStream is;
        if (or == null) {
            throw new MissingObjectException("blob", f.getId());
        }
        is = or.getInputStream();
        try {
            final FileOutputStream fos = new FileOutputStream(d);
            try {
                int r;
                while ((r = is.read(copyBuffer)) > 0) {
                    fos.write(copyBuffer, 0, r);
                }
            } finally {
                fos.close();
            }
        } finally {
            or.close();
        }
        super.visitFile(f);
    }
}
