package org.spearce.jgit.lib;

import java.util.Comparator;

public class SubtreeSorter implements Comparator
{
    public static final SubtreeSorter INSTANCE = new SubtreeSorter();

    public int compare(final Object o1, final Object o2)
    {
        final TreeEntry a = (TreeEntry) o1;
        final TreeEntry b = (TreeEntry) o2;
        final byte[] aName = a.getNameUTF8();
        final byte[] bName = b.getNameUTF8();

        for (int k = 0; k < aName.length && k < bName.length; k++)
        {
            if (aName[k] < bName[k])
                return -1;
            else if (aName[k] > bName[k])
                return 1;
        }

        if (aName.length < bName.length)
        {
            if (a instanceof Tree && '/' > bName[aName.length])
                return 1;
            return -1;
        }
        else if (aName.length == bName.length)
        {
            if (a instanceof Tree && b instanceof Tree)
                return 0;
            else if (a instanceof Tree)
                return 1;
            else if (b instanceof Tree)
                return -1;
            else
                return 0;
        }
        else
        {
            if (b instanceof Tree && aName[bName.length] < '/')
                return -1;
            return 1;
        }
    }
}
