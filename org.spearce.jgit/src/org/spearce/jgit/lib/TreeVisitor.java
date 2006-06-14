package org.spearce.jgit.lib;

import java.io.IOException;

public interface TreeVisitor
{
    public void startVisitTree(final Tree t) throws IOException;

    public void endVisitTree(final Tree t) throws IOException;

    public void visitFile(final FileTreeEntry f) throws IOException;

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException;
}
