package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import org.spearce.jgit.errors.CorruptObjectException;

public class UnpackedObjectReader extends ObjectReader
{
    private static final int MAX_TYPE_LEN = 16;

    private final String objectType;

    private final long objectSize;

    private InflaterInputStream inflater;

    public UnpackedObjectReader(final ObjectId id, final InputStream src)
        throws IOException
    {
        final StringBuffer typeBuf = new StringBuffer(MAX_TYPE_LEN);
        final String typeStr;
        long tempSize = 0;

        setId(id);
        inflater = new InflaterInputStream(src);

        for (;;)
        {
            final int c = inflater.read();
            if (' ' == c)
            {
                break;
            }
            else if (c < 'a' || c > 'z' || typeBuf.length() >= MAX_TYPE_LEN)
            {
                throw new CorruptObjectException(id, "bad type in header");
            }
            typeBuf.append((char) c);
        }

        typeStr = typeBuf.toString();
        if (Constants.TYPE_BLOB.equals(typeStr))
        {
            objectType = Constants.TYPE_BLOB;
        }
        else if (Constants.TYPE_TREE.equals(typeStr))
        {
            objectType = Constants.TYPE_TREE;
        }
        else if (Constants.TYPE_COMMIT.equals(typeStr))
        {
            objectType = Constants.TYPE_COMMIT;
        }
        else if (Constants.TYPE_TAG.equals(typeStr))
        {
            objectType = Constants.TYPE_TAG;
        }
        else
        {
            throw new CorruptObjectException(id, "invalid type: " + typeStr);
        }

        for (;;)
        {
            final int c = inflater.read();
            if (0 == c)
            {
                break;
            }
            else if (c < '0' || c > '9')
            {
                throw new CorruptObjectException(id, "bad length in header");
            }
            tempSize *= 10;
            tempSize += c - '0';
        }
        objectSize = tempSize;
    }

    public String getType()
    {
        return objectType;
    }

    public long getSize()
    {
        return objectSize;
    }

    public InputStream getInputStream()
    {
        if (inflater == null)
        {
            throw new IllegalStateException("Already closed.");
        }
        return inflater;
    }

    public void close() throws IOException
    {
        if (inflater != null)
        {
            inflater.close();
            inflater = null;
        }
    }
}
