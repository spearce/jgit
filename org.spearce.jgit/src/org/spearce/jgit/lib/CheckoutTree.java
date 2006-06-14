package org.spearce.jgit.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CheckoutTree extends TreeVisitorWithCurrentDirectory
{
    private final byte[] copyBuffer;

    public CheckoutTree(final File root)
    {
        super(root);
        copyBuffer = new byte[8192];
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        final File destFile = new File(getCurrentDirectory(), f.getName());
        final ObjectReader or = f.openReader();

        if (or == null)
        {
            throw new MissingObjectException(Constants.TYPE_BLOB, f.getId());
        }

        try
        {
            final InputStream is = or.getInputStream();
            try
            {
                final FileOutputStream fos = new FileOutputStream(destFile);
                try
                {
                    int r;
                    while ((r = is.read(copyBuffer)) > 0)
                    {
                        fos.write(copyBuffer, 0, r);
                    }
                }
                finally
                {
                    fos.close();
                }
            }
            finally
            {
                or.close();
            }
        }
        catch (IOException ioe)
        {
            destFile.delete();
            throw ioe;
        }
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        super.startVisitTree(t);
        getCurrentDirectory().mkdirs();
    }
}
