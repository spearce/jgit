package org.spearce.jgit.lib;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Constants
{
    private static final String HASH_FUNCTION = "SHA-1";

    public static final int OBJECT_ID_LENGTH;

    public static final String HEAD = "HEAD";

    public static final String TYPE_COMMIT = "commit";

    public static final String TYPE_BLOB = "blob";

    public static final String TYPE_TREE = "tree";

    public static final String TYPE_TAG = "tag";

    public static final String CHARACTER_ENCODING = "UTF-8";

    public static MessageDigest newMessageDigest()
    {
        try
        {
            return MessageDigest.getInstance(HASH_FUNCTION);
        }
        catch (NoSuchAlgorithmException nsae)
        {
            throw new RuntimeException("Required hash function "
                + HASH_FUNCTION
                + " not available.", nsae);
        }
    }

    public static byte[] encodeASCII(final long s)
    {
        return encodeASCII(Long.toString(s));
    }

    public static byte[] encodeASCII(final String s)
    {
        final byte[] r = new byte[s.length()];
        for (int k = r.length - 1; k >= 0; k--)
        {
            final char c = s.charAt(k);
            if (c > 127)
            {
                throw new IllegalArgumentException("Not ASCII string: " + s);
            }
            r[k] = (byte) c;
        }
        return r;
    }

    static
    {
        OBJECT_ID_LENGTH = newMessageDigest().getDigestLength();
    }

    private Constants()
    {
    }
}
