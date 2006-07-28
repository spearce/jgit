package org.spearce.egit.core.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.spearce.jgit.errors.SymlinksNotSupportedException;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.SymlinkTreeEntry;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeVisitorWithCurrentDirectory;

public class EnqueueWriteTree extends TreeVisitorWithCurrentDirectory
{
    private final CheckpointJob queue;

    private final ArrayList stack;

    private IContainer currentContainer;

    public EnqueueWriteTree(
        final IContainer sourceContainer,
        final CheckpointJob q)
    {
        super(sourceContainer.getLocation().toFile());
        queue = q;
        stack = new ArrayList(16);
        currentContainer = sourceContainer;
    }

    public void visitFile(final FileTreeEntry f)
    {
        final String name = f.getName();
        queue.enqueue(currentContainer.findMember(name), new File(
            getCurrentDirectory(),
            name), f);
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

    public void startVisitTree(final Tree t) throws IOException
    {
        super.startVisitTree(t);
        stack.add(currentContainer);
        if (!t.isRoot())
        {
            final IResource r = currentContainer.findMember(t.getName());
            if (r instanceof IContainer)
            {
                currentContainer = (IContainer) r;
            }
            else
            {
                currentContainer = (IContainer) r.getAdapter(IContainer.class);
                if (currentContainer == null)
                {
                    throw new IOException("What was a tree, now isn't: "
                        + t.getFullName());
                }
            }
        }
    }

    public void endVisitTree(final Tree t) throws IOException
    {
        super.endVisitTree(t);
        queue.enqueue(t);
        currentContainer = (IContainer) stack.remove(stack.size() - 1);
    }
}
