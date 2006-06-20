package org.spearce.egit.core.project;

import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.SymlinkTreeEntry;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeVisitor;

public class UpdateTreeFromWorkspace implements TreeVisitor
{
    private IContainer currentContainer;

    public UpdateTreeFromWorkspace(final IContainer root)
    {
        currentContainer = root;
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        if (!t.isRoot())
        {
            final IResource r = currentContainer.findMember(t.getName());
            IContainer c;

            if (r == null)
            {
                c = null;
            }
            else if (r instanceof IContainer)
            {
                c = (IContainer) r;
            }
            else
            {
                c = (IContainer) r.getAdapter(IContainer.class);
            }

            if (c == null || !c.exists())
            {
                t.delete();
            }
            else
            {
                currentContainer = c;
            }
        }
    }

    public void endVisitTree(final Tree t) throws IOException
    {
        if (t.getParent() != null)
        {
            currentContainer = currentContainer.getParent();
        }
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        final IResource r = currentContainer.findMember(f.getName());
        if (r == null || !r.exists())
        {
            f.delete();
        }
        else if (!(r instanceof IFile) && r.getAdapter(IFile.class) == null)
        {
            f.delete();
        }
        else
        {
            f.setModified();
        }
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
    }
}
