package org.spearce.jgit.lib;

import java.io.IOException;

public class MergedTree
{
    private static final int binarySearch(
        final TreeEntry[] entries,
        final int width,
        final byte[] nameUTF8,
        final int nameStart,
        final int nameEnd)
    {
        if (entries.length == 0)
            return -1;
        int high = entries.length / width;
        int low = 0;
        do
        {
            final int mid = (low + high) / 2;
            final int cmp;
            int ix = mid * width;
            while (entries[ix] == null)
                ix++;
            cmp = Tree.compareNames(
                entries[mid].getNameUTF8(),
                nameUTF8,
                nameStart,
                nameEnd);
            if (cmp < 0)
            {
                low = mid + 1;
            }
            else if (cmp == 0)
            {
                return mid;
            }
            else
            {
                high = mid;
            }
        }
        while (low < high);
        return -(low + 1);
    }

    private final Tree[] sources;

    private TreeEntry[] merged;

    public MergedTree(final Tree[] src) throws IOException
    {
        sources = src;
        remerge();
    }

    public TreeEntry[] findMember(final String s)
        throws IOException,
            MissingObjectException
    {
        return findMember(s.getBytes(Constants.CHARACTER_ENCODING), 0);
    }

    public TreeEntry[] findMember(final byte[] s, final int offset)
        throws IOException,
            MissingObjectException
    {
        int slash;
        int p;
        final TreeEntry[] r;

        for (slash = offset; slash < s.length && s[slash] != '/'; slash++)
            /* search for path component terminator */;
        p = binarySearch(merged, sources.length, s, offset, slash);
        if (p < 0)
        {
            return null;
        }

        p *= sources.length;
        r = new TreeEntry[sources.length];
        for (int j = 0; j < sources.length; j++, p++)
        {
            r[j] = merged[p];
        }

        if (slash < s.length)
        {
            boolean gotATree = false;
            for (int j = 0; j < sources.length; j++)
            {
                if (r[j] instanceof Tree)
                {
                    r[j] = ((Tree) r[j]).findMember(s, slash + 1);
                    gotATree = true;
                }
                else if (r[j] != null)
                {
                    r[j] = null;
                }
            }
            return gotATree ? r : null;
        }
        return r;
    }

    private void remerge() throws IOException
    {
        final int srcCnt = sources.length;
        final int[] treeIndexes = new int[srcCnt];
        final TreeEntry[][] entries = new TreeEntry[srcCnt][];
        int pos = 0;
        int done = 0;
        TreeEntry[] newm;

        for (int srcId = srcCnt; srcId >= 0; srcId--)
        {
            if (sources[srcId] != null)
            {
                final TreeEntry[] ents = sources[srcId].entries();
                entries[srcId] = ents;
                pos = Math.max(pos, ents.length);
                if (ents.length == 0)
                {
                    done++;
                }
            }
            else
            {
                entries[srcId] = Tree.EMPTY_TREE;
                done++;
            }
        }

        if (pos == 0)
        {
            merged = Tree.EMPTY_TREE;
            return;
        }

        newm = new TreeEntry[pos * srcCnt];
        for (pos = 0; done < srcCnt; pos += srcCnt)
        {
            byte[] minName = null;

            if ((pos + srcCnt) >= newm.length)
            {
                final TreeEntry[] largerm = new TreeEntry[newm.length * 2];
                for (int j = newm.length - 1; j >= 0; j--)
                {
                    largerm[j] = newm[j];
                }
                newm = largerm;
            }

            for (int srcId = 0; srcId < srcCnt; srcId++)
            {
                final int ti = treeIndexes[srcId];
                final TreeEntry[] ents = entries[srcId];
                final byte[] n = ents[ti].getNameUTF8();
                final int cmp;

                cmp = minName != null ? Tree.compareNames(n, minName) : -1;

                if (cmp < 0)
                {
                    minName = n;
                    for (int j = srcId - 1; j >= 0; j--)
                    {
                        if (newm[pos + j] != null)
                        {
                            newm[pos + j] = null;
                            if (treeIndexes[j]-- == entries[j].length)
                            {
                                done--;
                            }
                        }
                    }
                }

                if (cmp <= 0)
                {
                    newm[pos + srcId] = ents[ti];
                    if (++treeIndexes[srcId] == ents.length)
                    {
                        done++;
                    }
                }
            }
        }

        if (newm.length == pos)
        {
            merged = newm;
        }
        else
        {
            merged = new TreeEntry[pos];
            for (int j = pos - 1; j >= 0; j--)
            {
                merged[j] = newm[j];
            }
        }
    }
}
