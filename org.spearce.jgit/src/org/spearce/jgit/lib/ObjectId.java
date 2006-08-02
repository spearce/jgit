/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public class ObjectId implements Comparable
{
    private static final ObjectId ZEROID;

    private static final String ZEROID_STR;

    static
    {
        ZEROID = new ObjectId(new byte[Constants.OBJECT_ID_LENGTH]);
        ZEROID_STR = ZEROID.toString();
    }

    public static final boolean isId(final String id)
    {
        if (id.length() != (2 * Constants.OBJECT_ID_LENGTH))
        {
            return false;
        }

        for (int k = id.length() - 1; k >= 0; k--)
        {
            final char c = id.charAt(k);
            if ('0' <= c && c <= '9')
            {
                continue;
            }
            else if ('a' <= c && c <= 'f')
            {
                continue;
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    public static String toString(final ObjectId i)
    {
        return i != null ? i.toString() : ZEROID_STR;
    }

    private static int compare(final byte[] a, final byte[] b)
    {
        for (int k = 0; k < a.length && k < b.length; k++)
        {
            final int ak = a[k] & 0xff;
            final int bk = b[k] & 0xff;
            if (ak < bk)
                return -1;
            else if (ak > bk)
                return 1;
        }
        return a.length == b.length ? 0 : a.length < b.length ? -1 : 1;
    }

    private final byte[] id;

    public ObjectId(final String i)
    {
        if (i.length() != (2 * Constants.OBJECT_ID_LENGTH))
        {
            throw new IllegalArgumentException("Invalid id \""
                + i
                + "\"; length = "
                + i.length());
        }

        id = new byte[Constants.OBJECT_ID_LENGTH];
        for (int j = 0, k = 0; k < Constants.OBJECT_ID_LENGTH; k++)
        {
            final char c1 = i.charAt(j++);
            final char c2 = i.charAt(j++);
            int b;

            if ('0' <= c1 && c1 <= '9')
            {
                b = c1 - '0';
            }
            else if ('a' <= c1 && c1 <= 'f')
            {
                b = c1 - 'a' + 10;
            }
            else
            {
                throw new IllegalArgumentException("Invalid id: " + i);
            }

            b <<= 4;

            if ('0' <= c2 && c2 <= '9')
            {
                b |= c2 - '0';
            }
            else if ('a' <= c2 && c2 <= 'f')
            {
                b |= c2 - 'a' + 10;
            }
            else
            {
                throw new IllegalArgumentException("Invalid id: " + i);
            }

            id[k] = (byte) b;
        }
    }

    public ObjectId(final byte[] i)
    {
        id = i;
    }

    public byte[] getBytes()
    {
        return id;
    }

    public int compareTo(final byte[] b)
    {
        return b != null ? compare(id, b) : 1;
    }

    public int compareTo(final ObjectId b)
    {
        return b != null ? compare(id, b.id) : 1;
    }

    public int compareTo(final Object o)
    {
        if (o instanceof ObjectId)
        {
            return compare(id, ((ObjectId) o).id);
        }
        else if (o instanceof byte[])
        {
            return compare(id, (byte[]) o);
        }
        return 1;
    }

    public int hashCode()
    {
        int r = 0;
        for (int k = id.length - 1; k >= 0; k--)
        {
            r *= 31;
            r += id[k];
        }
        return r;
    }

    public boolean equals(final ObjectId o)
    {
        return compareTo(o) == 0;
    }

    public boolean equals(final Object o)
    {
        return compareTo(o) == 0;
    }

    public void copyTo(final OutputStream w) throws IOException
    {
        for (int k = 0; k < id.length; k++)
        {
            final int b = id[k];
            final int b1 = (b >> 4) & 0xf;
            final int b2 = b & 0xf;
            w.write(b1 < 10 ? (char) ('0' + b1) : (char) ('a' + b1 - 10));
            w.write(b2 < 10 ? (char) ('0' + b2) : (char) ('a' + b2 - 10));
        }
    }

    public void copyTo(final Writer w) throws IOException
    {
        for (int k = 0; k < id.length; k++)
        {
            final int b = id[k];
            final int b1 = (b >> 4) & 0xf;
            final int b2 = b & 0xf;
            w.write(b1 < 10 ? (char) ('0' + b1) : (char) ('a' + b1 - 10));
            w.write(b2 < 10 ? (char) ('0' + b2) : (char) ('a' + b2 - 10));
        }
    }

    public String toString()
    {
        final StringBuffer r = new StringBuffer(2 * id.length);
        for (int k = 0; k < id.length; k++)
        {
            final int b = id[k];
            final int b1 = (b >> 4) & 0xf;
            final int b2 = b & 0xf;
            r.append(b1 < 10 ? (char) ('0' + b1) : (char) ('a' + b1 - 10));
            r.append(b2 < 10 ? (char) ('0' + b2) : (char) ('a' + b2 - 10));
        }
        return r.toString();
    }
}
