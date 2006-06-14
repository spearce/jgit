package org.spearce.jgit.lib;

import java.io.IOException;

public class FileTreeEntry extends TreeEntry
{
    private FileMode mode;

    public FileTreeEntry(
        final Tree parent,
        final ObjectId id,
        final byte[] nameUTF8,
        final boolean execute)
    {
        super(parent, id, nameUTF8);
        setExecutable(execute);
    }

    public FileMode getMode()
    {
        return mode;
    }

    public boolean isExecutable()
    {
        return getMode().equals(FileMode.EXECUTABLE_FILE);
    }

    public void setExecutable(final boolean execute)
    {
        mode = execute ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;
    }

    public ObjectReader openReader() throws IOException
    {
        return getRepository().openBlob(getId());
    }

    public void accept(final TreeVisitor tv, final int flags)
        throws IOException
    {
        if ((MODIFIED_ONLY & flags) == MODIFIED_ONLY && !isModified())
        {
            return;
        }

        tv.visitFile(this);
    }

    public String toString()
    {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(' ');
        r.append(isExecutable() ? 'X' : 'F');
        r.append(' ');
        r.append(getFullName());
        return r.toString();
    }
}
