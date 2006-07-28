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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public abstract class TreeEntry
{
    public static final int MODIFIED_ONLY = 1 << 0;

    public static final int LOADED_ONLY = 1 << 1;

    public static final int CONCURRENT_MODIFICATION = 1 << 2;

    private byte[] nameUTF8;

    private Tree parent;

    private ObjectId id;

    protected TreeEntry(
        final Tree myParent,
        final ObjectId myId,
        final byte[] myNameUTF8)
    {
        nameUTF8 = myNameUTF8;
        parent = myParent;
        id = myId;
    }

    public Tree getParent()
    {
        return parent;
    }

    public void delete()
    {
        getParent().removeEntry(this);
        detachParent();
    }

    public void detachParent()
    {
        parent = null;
    }

    void attachParent(final Tree p)
    {
        parent = p;
    }

    public Repository getRepository()
    {
        return getParent().getRepository();
    }

    public byte[] getNameUTF8()
    {
        return nameUTF8;
    }

    public String getName()
    {
        try
        {
            return nameUTF8 != null ? new String(
                nameUTF8,
                Constants.CHARACTER_ENCODING) : null;
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new RuntimeException("JVM doesn't support "
                + Constants.CHARACTER_ENCODING, uee);
        }
    }

    public void rename(final String n) throws IOException
    {
        rename(n.getBytes(Constants.CHARACTER_ENCODING));
    }

    public void rename(final byte[] n) throws IOException
    {
        final Tree t = getParent();
        if (t != null)
        {
            delete();
        }
        nameUTF8 = n;
        if (t != null)
        {
            t.addEntry(this);
        }
    }

    public boolean isModified()
    {
        return getId() == null;
    }

    public void setModified()
    {
        setId(null);
    }

    public ObjectId getId()
    {
        return id;
    }

    public void setId(final ObjectId n)
    {
        // If we have a parent and our id is being cleared or changed then force
        // the parent's id to become unset as it depends on our id.
        //
        final Tree p = getParent();
        if (p != null && id != n)
        {
            if ((id == null && n != null)
                || (id != null && n == null)
                || !id.equals(n))
            {
                p.setId(null);
            }
        }

        id = n;
    }

    public String getFullName()
    {
        final StringBuffer r = new StringBuffer();
        appendFullName(r);
        return r.toString();
    }

    public void accept(final TreeVisitor tv) throws IOException
    {
        accept(tv, 0);
    }

    public abstract void accept(TreeVisitor tv, int flags) throws IOException;

    public abstract FileMode getMode();

    private void appendFullName(final StringBuffer r)
    {
        final TreeEntry p = getParent();
        final String n = getName();
        if (p != null)
        {
            p.appendFullName(r);
            if (r.length() > 0)
            {
                r.append('/');
            }
        }
        if (n != null)
        {
            r.append(n);
        }
    }
}
