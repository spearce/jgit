package org.spearce.jgit.lib;

import java.io.UnsupportedEncodingException;
import java.util.Comparator;

public class TreeNameComparator implements Comparator
{
    public int compare(final Object arg0, final Object arg1)
    {
        return ObjectId.compare(toUTF8(arg0), toUTF8(arg1));
    }

    private static byte[] toUTF8(final Object a)
    {
        try
        {
            if (a instanceof TreeEntry)
            {
                return ((TreeEntry) a).getNameUTF8();
            }
            else if (a instanceof String)
            {
                return ((String) a).getBytes("UTF-8");
            }
            else
            {
                throw new IllegalArgumentException("Not a TreeEntry"
                    + " or String: "
                    + a);
            }
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new RuntimeException("JVM does not support UTF-8."
                + "  It should have.  I don't know why it doesn't.", uee);
        }
    }
}
