package org.spearce.jgit.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Tree extends TreeEntry implements Treeish
{
    public static final TreeEntry[] EMPTY_TREE = {};

    public static final int compareNames(final byte[] a, final byte[] b)
    {
        return compareNames(a, b, 0, b.length);
    }

    public static final int compareNames(
        final byte[] a,
        final byte[] nameUTF8,
        final int nameStart,
        final int nameEnd)
    {
        for (int j = 0, k = nameStart; j < a.length && k < nameEnd; j++, k++)
        {
            final int aj = a[j] & 0xff;
            final int bk = nameUTF8[k] & 0xff;
            if (aj < bk)
                return -1;
            else if (aj > bk)
                return 1;
        }

        final int namelength = nameEnd - nameStart;
        if (a.length == namelength)
            return 0;
        else if (a.length < namelength)
            return -1;
        else
            return 1;
    }

    private static final int binarySearch(
        final TreeEntry[] entries,
        final byte[] nameUTF8,
        final int nameStart,
        final int nameEnd)
    {
        if (entries.length == 0)
            return -1;
        int high = entries.length;
        int low = 0;
        do
        {
            final int mid = (low + high) / 2;
            final int cmp = compareNames(
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

    private final Repository db;

    private TreeEntry[] contents;

    public Tree(final Repository repo)
    {
        super(null, null, null);
        db = repo;
        contents = EMPTY_TREE;
    }

    public Tree(final Repository repo, final ObjectId myId, final InputStream is)
        throws IOException
    {
        super(null, myId, null);
        db = repo;
        readTree(is);
    }

    private Tree(final Tree parent, final byte[] nameUTF8)
    {
        super(parent, null, nameUTF8);
        db = parent.getRepository();
        contents = EMPTY_TREE;
    }

    public Tree(final Repository r, final ObjectId id, final byte[] nameUTF8)
    {
        super(null, id, nameUTF8);
        db = r;
    }

    public Tree(final Tree parent, final ObjectId id, final byte[] nameUTF8)
    {
        super(parent, id, nameUTF8);
        db = parent.getRepository();
    }

    public FileMode getMode()
    {
        return FileMode.TREE;
    }

    public boolean isRoot()
    {
        return getParent() == null;
    }

    public Repository getRepository()
    {
        return db;
    }

    public final ObjectId getTreeId()
    {
        return getId();
    }

    public final Tree getTree()
    {
        return this;
    }

    public boolean isLoaded()
    {
        return contents != null;
    }

    public void unload()
    {
        if (isModified())
        {
            throw new IllegalStateException("Cannot unload a modified tree.");
        }
        contents = null;
    }

    public FileTreeEntry addFile(final String name)
        throws IOException,
            MissingObjectException
    {
        final FileTreeEntry n;
        ensureLoaded();
        n = new FileTreeEntry(this, null, name
            .getBytes(Constants.CHARACTER_ENCODING), false);
        addEntry(n);
        return n;
    }

    public Tree addTree(final String name)
        throws IOException,
            MissingObjectException
    {
        final Tree n;
        ensureLoaded();
        n = new Tree(this, name.getBytes(Constants.CHARACTER_ENCODING));
        addEntry(n);
        return n;
    }

    public Tree linkTree(final String name, final ObjectId id)
        throws IOException,
            MissingObjectException
    {
        final Tree n;
        ensureLoaded();
        n = new Tree(this, id, name.getBytes(Constants.CHARACTER_ENCODING));
        addEntry(n);
        return n;
    }

    private void addEntry(final TreeEntry e)
    {
        final TreeEntry[] c = contents;
        int p = binarySearch(c, e.getNameUTF8(), 0, e.getNameUTF8().length);
        if (p >= 0)
        {
            c[p] = e;
        }
        else
        {
            final TreeEntry[] n = new TreeEntry[c.length + 1];
            p = -(p + 1);
            for (int k = c.length - 1; k >= p; k--)
            {
                n[k + 1] = c[k];
            }
            n[p] = e;
            for (int k = p - 1; k >= 0; k--)
            {
                n[k] = c[k];
            }
            contents = n;
        }
        setModified();
    }

    public int entryCount() throws IOException, MissingObjectException
    {
        ensureLoaded();
        return contents.length;
    }

    public TreeEntry[] entries() throws IOException, MissingObjectException
    {
        ensureLoaded();
        return contents;
    }

    public void setEntries(final TreeEntry[] c)
    {
        contents = c;
    }

    public TreeEntry findMember(final String s)
        throws IOException,
            MissingObjectException
    {
        return findMember(s.getBytes(Constants.CHARACTER_ENCODING), 0);
    }

    public TreeEntry findMember(final byte[] s, final int offset)
        throws IOException,
            MissingObjectException
    {
        int slash;
        int p;
        final TreeEntry r;

        ensureLoaded();
        for (slash = offset; slash < s.length && s[slash] != '/'; slash++)
            /* search for path component terminator */;
        p = binarySearch(contents, s, offset, slash);
        if (p < 0)
        {
            return null;
        }

        r = contents[p];
        if (slash < s.length)
        {
            return r instanceof Tree
                ? ((Tree) r).findMember(s, slash + 1)
                : null;
        }
        return r;
    }

    public void accept(final TreeVisitor tv, final int flags)
        throws IOException
    {
        if ((MODIFIED_ONLY & flags) == MODIFIED_ONLY && !isModified())
        {
            return;
        }

        if ((LOADED_ONLY & flags) == LOADED_ONLY && !isLoaded())
        {
            tv.startVisitTree(this);
            tv.endVisitTree(this);
            return;
        }

        ensureLoaded();
        tv.startVisitTree(this);
        final TreeEntry[] c = contents;
        for (int k = 0; k < c.length; k++)
        {
            c[k].accept(tv, flags);
        }
        tv.endVisitTree(this);
    }

    private void ensureLoaded() throws IOException, MissingObjectException
    {
        if (!isLoaded())
        {
            final ObjectReader or = db.openTree(getId());
            if (or == null)
            {
                throw new MissingObjectException(Constants.TYPE_TREE, getId());
            }
            try
            {
                readTree(or.getInputStream());
            }
            finally
            {
                or.close();
            }
        }
    }

    private void readTree(final InputStream is) throws IOException
    {
        TreeEntry[] temp = new TreeEntry[64];
        int nextIndex = 0;

        for (;;)
        {
            int c;
            int mode;
            final ByteArrayOutputStream nameBuf;
            final byte[] entId;
            final byte[] name;
            final ObjectId id;
            final TreeEntry ent;
            int entIdLen;

            c = is.read();
            if (c == -1)
            {
                break;
            }
            if (c < '0' || c > '7')
            {
                throw new CorruptObjectException(getId(), "invalid mode");
            }
            mode = c - '0';
            for (;;)
            {
                c = is.read();
                if (' ' == c)
                {
                    break;
                }
                if (c < '0' || c > '7')
                {
                    throw new CorruptObjectException(getId(), "invalid mode");
                }
                mode *= 8;
                mode += c - '0';
            }

            nameBuf = new ByteArrayOutputStream(128);
            for (;;)
            {
                c = is.read();
                if (c == -1)
                {
                    throw new CorruptObjectException(getId(), "unexpected eof");
                }
                if (0 == c)
                {
                    break;
                }
                nameBuf.write(c);
            }

            entId = new byte[Constants.OBJECT_ID_LENGTH];
            entIdLen = 0;
            while ((c = is.read(entId, entIdLen, entId.length - entIdLen)) > 0)
            {
                entIdLen += c;
            }
            if (entIdLen != entId.length)
            {
                throw new CorruptObjectException(getId(), "missing hash");
            }

            id = new ObjectId(entId);
            name = nameBuf.toByteArray();

            if (FileMode.REGULAR_FILE.equals(mode))
            {
                ent = new FileTreeEntry(this, id, name, false);
            }
            else if (FileMode.EXECUTABLE_FILE.equals(mode))
            {
                ent = new FileTreeEntry(this, id, name, true);
            }
            else if (FileMode.TREE.equals(mode))
            {
                ent = new Tree(this, id, name);
            }
            else if (FileMode.SYMLINK.equals(mode))
            {
                ent = new SymlinkTreeEntry(this, id, name);
            }
            else
            {
                throw new CorruptObjectException(getId(), "Invalid mode: "
                    + Integer.toOctalString(mode));
            }

            if (nextIndex == temp.length)
            {
                final TreeEntry[] n = new TreeEntry[temp.length << 1];
                for (int k = nextIndex - 1; k >= 0; k--)
                {
                    n[k] = temp[k];
                }
                temp = n;
            }

            // Make damn sure the tree object is formatted properly as we really
            // depend on it later on when we edit the tree contents. This should
            // be pretty quick to validate on the fly as we read the tree in and
            // it should never be wrong.
            // 
            if (nextIndex > 0
                && compareNames(temp[nextIndex - 1].getNameUTF8(), name) >= 0)
            {
                throw new CorruptObjectException(
                    getId(),
                    "Tree is not sorted according to object names.");
            }

            temp[nextIndex++] = ent;
        }

        if (nextIndex == temp.length)
        {
            contents = temp;
        }
        else
        {
            final TreeEntry[] n = new TreeEntry[nextIndex];
            for (int k = nextIndex - 1; k >= 0; k--)
            {
                n[k] = temp[k];
            }
            contents = n;
        }
    }

    public String toString()
    {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(" T ");
        r.append(getFullName());
        return r.toString();
    }
}