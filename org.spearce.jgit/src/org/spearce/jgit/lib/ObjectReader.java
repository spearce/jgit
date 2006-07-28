/*
 *    Copyright 2006 Shawn Pearce <spearce@spearce.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                md.update(Constants.encodeASCII(getType()));
                md.update((byte) ' ');
                md.update(Constants.encodeASCII(getSize()));
                md.update((byte) 0);
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
            Constants.CHARACTER_ENCODING));
    }

    public abstract String getType() throws IOException;

    public abstract long getSize() throws IOException;

    public abstract InputStream getInputStream() throws IOException;

    public abstract void close() throws IOException;
}
