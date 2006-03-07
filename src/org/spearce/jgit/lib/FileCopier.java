package org.spearce.jgit.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class FileCopier {
    public void copyOut(final Tree src, final File dest) throws IOException {
        dest.mkdirs();
        copyOut(src, dest, new byte[8192]);
    }

    protected void copyOut(final Tree src, final File dest,
            final byte[] copyBuffer) throws IOException {
        final Iterator i;

        i = src.getTreeEntries().iterator();
        while (i.hasNext()) {
            final Tree.Entry e = (Tree.Entry) i.next();
            final File f = new File(dest, e.getName());

            if (e.isTree()) {
                f.mkdir();
                copyOut(e.getTree(), f, copyBuffer);
            } else if (e.isSymlink()) {
                // TODO: We don't handle symlinks right now.
            } else {
                final ObjectReader or = e.openBlob();
                final InputStream is;
                if (or == null) {
                    throw new CorruptObjectException("Missing blob "
                            + e.getId());
                }
                is = or.getInputStream();
                try {
                    final FileOutputStream fos = new FileOutputStream(f);
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
            }
        }
    }
}
