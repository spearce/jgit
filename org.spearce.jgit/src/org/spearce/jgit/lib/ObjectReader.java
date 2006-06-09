package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

public abstract class ObjectReader
{
    private ObjectId objectId;

    public ObjectId getId() throws IOException
    {
        if (objectId == null)
        {
            final MessageDigest md = Constants.newMessageDigest();
            final InputStream is = getInputStream();
            try
            {
                final byte[] buf = new byte[2048];
                int r;
                md.update((getType() + " " + getSize() + "\0")
                    .getBytes("UTF-8"));
                while ((r = is.read(buf)) > 0)
                {
                    md.update(buf, 0, r);
                }
            }
            finally
            {
                is.close();
            }
            objectId = new ObjectId(md.digest());
        }
        return objectId;
    }

    protected void setId(final ObjectId id)
    {
        if (objectId != null)
        {
            throw new IllegalStateException("Id already set.");
        }
        objectId = id;
    }

    public BufferedReader getBufferedReader()
        throws UnsupportedEncodingException,
            IOException
    {
        return new BufferedReader(new InputStreamReader(
            getInputStream(),
            "UTF-8"));
    }

    public abstract String getType() throws IOException;

    public abstract long getSize() throws IOException;

    public abstract InputStream getInputStream() throws IOException;

    public abstract void close() throws IOException;
}
