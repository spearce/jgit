package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public abstract class TreeVisitorWithCurrentDirectory implements TreeVisitor
{
    private final ArrayList stack;

    private File currentDirectory;

    protected TreeVisitorWithCurrentDirectory(final File rootDirectory)
    {
        stack = new ArrayList(16);
        currentDirectory = rootDirectory;
    }

    protected File getCurrentDirectory()
    {
        return currentDirectory;
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        stack.add(currentDirectory);
        if (!t.isRoot())
        {
            currentDirectory = new File(currentDirectory, t.getName());
        }
    }

    public void endVisitTree(final Tree t) throws IOException
    {
        currentDirectory = (File) stack.remove(stack.size() - 1);
    }
}
