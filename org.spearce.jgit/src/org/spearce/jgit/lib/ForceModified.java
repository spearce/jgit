package org.spearce.jgit.lib;

import java.io.IOException;

public class ForceModified implements TreeVisitor
{
    public void startVisitTree(final Tree t) throws IOException
    {
        t.setModified();
    }

    public void endVisitTree(final Tree t) throws IOException
    {
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        f.setModified();
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
    }
}
