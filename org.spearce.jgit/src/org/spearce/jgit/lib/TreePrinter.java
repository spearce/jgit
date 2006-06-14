package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.PrintStream;

public class TreePrinter implements TreeVisitor
{
    private final PrintStream dest;

    public TreePrinter(final PrintStream x)
    {
        dest = x;
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        dest.println(f);
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
        dest.println(s);
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        dest.println(t);
    }

    public void endVisitTree(final Tree t) throws IOException
    {
    }
}
