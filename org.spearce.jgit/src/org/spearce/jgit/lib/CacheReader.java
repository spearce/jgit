package org.spearce.jgit.lib;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class CacheReader
{
    private final DataInputStream dis;

    public CacheReader(final InputStream is) throws IOException
    {
        dis = new DataInputStream(is);
        if (!CacheWriter.CACHE_SIGNATURE.equals(dis.readUTF()))
        {
            throw new IOException("Not a cache stream.");
        }
    }

    public Tree read(final Repository db) throws IOException
    {
        return (Tree) readEntry(db, null);
    }

    private TreeEntry readEntry(final Repository db, final Tree parent)
        throws IOException
    {
        int header;
        final ObjectId id;
        final int nameLen;
        final byte[] nameUTF8;

        header = dis.read();
        if (header == -1)
        {
            throw new EOFException();
        }

        if ((header & 1) == 1)
        {
            final byte[] rawid = new byte[Constants.OBJECT_ID_LENGTH];
            dis.readFully(rawid);
            id = new ObjectId(rawid);
        }
        else
        {
            id = null;
        }
        header >>= 1;

        nameLen = dis.readUnsignedShort();
        if (nameLen > 0)
        {
            nameUTF8 = new byte[nameLen];
            dis.readFully(nameUTF8);
        }
        else
        {
            nameUTF8 = null;
        }

        switch (header & 0xf0)
        {
        case 0x10:
        {
            final Tree t = parent != null
                ? new Tree(parent, id, nameUTF8)
                : new Tree(db, id, nameUTF8);
            if ((header & 1) == 1)
            {
                final TreeEntry[] contents = new TreeEntry[dis.readInt()];
                for (int k = 0; k < contents.length; k++)
                {
                    contents[k] = readEntry(db, t);
                }
                t.setEntries(contents);
            }
            return t;
        }

        case 0x20:
            return new FileTreeEntry(parent, id, nameUTF8, (header & 1) == 1);

        case 0x30:
            return new SymlinkTreeEntry(parent, id, nameUTF8);

        default:
            throw new IOException("Cache stream is corrupt.");
        }
    }
}
