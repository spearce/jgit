package org.spearce.jgit.lib;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CacheWriter implements TreeVisitor
{
    public static final String CACHE_SIGNATURE = "JGITCACHE 1\n";

    private final DataOutputStream dos;

    public CacheWriter(final OutputStream os) throws IOException
    {
        dos = new DataOutputStream(os);
        dos.writeUTF(CACHE_SIGNATURE);
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        if (t.isLoaded())
        {
            header(0x11, t);
            dos.writeInt(t.entryCount());
        }
        else
        {
            header(0x10, t);
        }
    }

    public void endVisitTree(final Tree t) throws IOException
    {
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        header(f.isExecutable() ? 0x21 : 0x20, f);
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
        header(0x30, s);
    }

    private void header(final int t, final TreeEntry e) throws IOException
    {
        final ObjectId id = e.getId();
        final byte[] n = e.getNameUTF8();

        if (id != null)
        {
            dos.write(t << 1 | 1);
            dos.write(id.getBytes());
        }
        else
        {
            dos.write(t << 1);
        }

        if (n == null || n.length == 0)
        {
            dos.writeShort(0);
        }
        else if (n.length < (1 << 16))
        {
            dos.writeShort(n.length);
            dos.write(n);
        }
        else
        {
            throw new IOException("TreeEntry name exceeds "
                + (1 << 16)
                + " bytes in entry.");
        }
    }
}
