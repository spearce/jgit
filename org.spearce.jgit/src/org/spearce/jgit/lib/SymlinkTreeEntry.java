package org.spearce.jgit.lib;

import java.io.IOException;

public class SymlinkTreeEntry extends TreeEntry
{
    private static final long serialVersionUID = 1L;

    public SymlinkTreeEntry(
        final Tree parent,
        final ObjectId id,
        final byte[] nameUTF8)
    {
        super(parent, id, nameUTF8);
    }

    public FileMode getMode()
    {
        return FileMode.SYMLINK;
    }

    public void accept(final TreeVisitor tv, final int flags)
        throws IOException
    {
        if ((MODIFIED_ONLY & flags) == MODIFIED_ONLY && !isModified())
        {
            return;
        }

        tv.visitSymlink(this);
    }

    public String toString()
    {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(" S ");
        r.append(getFullName());
        return r.toString();
    }
}
