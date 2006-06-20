package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

import org.spearce.jgit.errors.SymlinksNotSupportedException;

public class WriteTree extends TreeVisitorWithCurrentDirectory
{
    private final ObjectWriter ow;

    public WriteTree(final File sourceDirectory, final Repository db)
    {
        super(sourceDirectory);
        ow = new ObjectWriter(db);
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        f.setId(ow.writeBlob(new File(getCurrentDirectory(), f.getName())));
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
        if (s.isModified())
        {
            throw new SymlinksNotSupportedException("Symlink \""
                + s.getFullName()
                + "\" cannot be written as the link target"
                + " cannot be read from within Java.");
        }
    }

    public void endVisitTree(final Tree t) throws IOException
    {
        super.endVisitTree(t);
        t.setId(ow.writeTree(t));
    }
}
