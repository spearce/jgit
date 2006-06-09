package org.spearce.jgit.lib;

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
        if (id.length() != 2 * Constants.OBJECT_ID_LENGTH)
        {
            return false;
        }
        for (int k = 0; k < 2 * Constants.OBJECT_ID_LENGTH; k++)
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

    public static int compare(final byte[] a, final byte[] b)
    {
        for (int k = 0; k < a.length && k < b.length; k++)
        {
            final int ak = a[k] & 0xff;
            final int bk = b[k] & 0xff;
            if (ak < bk)
            {
                return -1;
            }
            else if (ak > bk)
            {
                return 1;
            }
        }

        if (a.length < b.length)
        {
            return -1;
        }
        else if (a.length == b.length)
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }

    private final byte[] id;

    public ObjectId(final String i)
    {
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
            else
            {
                b = c1 - 'a' + 10;
            }
            b <<= 4;
            if ('0' <= c2 && c2 <= '9')
            {
                b |= c2 - '0';
            }
            else
            {
                b |= c2 - 'a' + 10;
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

    public int compareTo(final Object o)
    {
        if (o instanceof byte[])
        {
            return compare(id, (byte[]) o);
        }
        return compare(id, ((ObjectId) o).id);
    }

    public int hashCode()
    {
        int r = 0;
        for (int k = 0; k < id.length; k++)
        {
            r *= 31;
            r += id[k];
        }
        return r;
    }

    public boolean equals(final Object o)
    {
        if (o instanceof ObjectId)
        {
            final byte[] o_id = ((ObjectId) o).id;
            if (o_id.length != id.length)
            {
                return false;
            }
            for (int k = 0; k < id.length; k++)
            {
                if (id[k] != o_id[k])
                {
                    return false;
                }
            }
            return true;
        }
        return false;
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
