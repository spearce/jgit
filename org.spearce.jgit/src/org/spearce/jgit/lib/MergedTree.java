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

    private MergedTree[] subtrees;

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
        int pos = merged != null ? merged.length / srcCnt : 0;
        int done = 0;
        int treeId;
        TreeEntry[] newMerged;
        MergedTree[] newSubtrees = null;

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

        if (done == srcCnt)
        {
            merged = Tree.EMPTY_TREE;
            subtrees = new MergedTree[0];
            return;
        }

        newMerged = new TreeEntry[pos * srcCnt];
        for (pos = 0, treeId = 0; done < srcCnt; pos += srcCnt, treeId++)
        {
            byte[] minName = null;
            boolean mergeCurrentTree = false;

            if ((pos + srcCnt) >= newMerged.length)
            {
                final TreeEntry[] t = new TreeEntry[newMerged.length * 2];
                for (int j = newMerged.length - 1; j >= 0; j--)
                {
                    t[j] = newMerged[j];
                }
                newMerged = t;
            }

            for (int srcId = 0; srcId < srcCnt; srcId++)
            {
                final int ti = treeIndexes[srcId];
                final TreeEntry[] ents = entries[srcId];
                final TreeEntry thisEntry = ents[ti];
                final int cmp = minName == null ? -1 : Tree.compareNames(
                    thisEntry.getNameUTF8(),
                    minName);

                if (cmp < 0)
                {
                    minName = thisEntry.getNameUTF8();
                    mergeCurrentTree = false;
                    for (int j = srcId - 1; j >= 0; j--)
                    {
                        if (newMerged[pos + j] != null)
                        {
                            newMerged[pos + j] = null;
                            if (treeIndexes[j]-- == entries[j].length)
                            {
                                done--;
                            }
                        }
                    }
                }

                if (cmp <= 0)
                {
                    newMerged[pos + srcId] = thisEntry;
                    if (thisEntry instanceof Tree)
                    {
                        if (srcId == 0)
                        {
                            mergeCurrentTree = true;
                        }
                        else if (srcId == 1)
                        {
                            final TreeEntry l = newMerged[pos];
                            mergeCurrentTree = !(l instanceof Tree)
                                || l.getId() == null
                                || l.getId().equals(thisEntry.getId());
                        }
                        else if (!mergeCurrentTree)
                        {
                            final TreeEntry l = newMerged[pos + srcId - 1];
                            mergeCurrentTree = !(l instanceof Tree)
                                || l.getId() == null
                                || l.getId().equals(thisEntry.getId());
                        }
                    }
                    if (++treeIndexes[srcId] == ents.length)
                    {
                        done++;
                    }
                }
            }

            if (mergeCurrentTree)
            {
                final Tree[] tmp = new Tree[srcCnt];
                for (int srcId = srcCnt - 1; srcId >= 0; srcId--)
                {
                    final TreeEntry t = newMerged[pos + srcId];
                    if (t instanceof Tree)
                    {
                        tmp[srcId] = (Tree) t;
                    }
                }

                if (newSubtrees == null)
                {
                    newSubtrees = new MergedTree[treeId + 1];
                }
                else if (treeId >= newSubtrees.length)
                {
                    final MergedTree[] s = new MergedTree[Math.max(
                        treeId + 1,
                        newSubtrees.length * 2)];
                    for (int j = newSubtrees.length - 1; j >= 0; j--)
                    {
                        s[j] = newSubtrees[j];
                    }
                    newSubtrees = s;
                }
                newSubtrees[treeId] = new MergedTree(tmp);
            }
        }

        if (newMerged.length == pos)
        {
            merged = newMerged;
        }
        else
        {
            merged = new TreeEntry[pos];
            for (int j = pos - 1; j >= 0; j--)
            {
                merged[j] = newMerged[j];
            }
        }

        if (newSubtrees == null || newSubtrees.length == treeId)
        {
            subtrees = newSubtrees;
        }
        else
        {
            subtrees = new MergedTree[treeId];
            for (int j = treeId - 1; j >= 0; j--)
            {
                subtrees[j] = newSubtrees[j];
            }
        }
    }
}
