package org.spearce.egit.core.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.spearce.egit.core.CoreText;

public class RepositoryFinder
{
    private final IProject proj;

    private final Collection results;

    public RepositoryFinder(final IProject p)
    {
        proj = p;
        results = new ArrayList();
    }

    public Collection find(IProgressMonitor m) throws CoreException
    {
        if (m == null)
        {
            m = new NullProgressMonitor();
        }
        find(m, proj);
        return results;
    }

    private void find(final IProgressMonitor m, final IContainer c)
        throws CoreException
    {
        final IPath loc = c.getLocation();

        m.beginTask("", 101);
        m.subTask(CoreText.RepositoryFinder_finding);
        try
        {
            if (loc != null)
            {
                final File fsLoc = loc.toFile();
                final File ownCfg = configFor(fsLoc);
                final IResource[] children;

                if (ownCfg.isFile())
                {
                    register(c, ownCfg.getParentFile(), null);
                }
                else if (c.isLinked() || c instanceof IProject)
                {
                    String s = fsLoc.getName();
                    File p = fsLoc.getParentFile();
                    while (p != null)
                    {
                        final File pCfg = configFor(p);
                        if (pCfg.isFile())
                        {
                            register(c, pCfg.getParentFile(), s);
                            break;
                        }
                        s = fsLoc.getName() + "/" + s;
                        p = p.getParentFile();
                    }
                }
                m.worked(1);

                children = c.members();
                if (children != null && children.length > 0)
                {
                    final int scale = 100 / children.length;
                    for (int k = 0; k < children.length; k++)
                    {
                        final IResource o = children[k];
                        if (o instanceof IContainer
                            && !o.getName().equals(".git"))
                        {
                            find(
                                new SubProgressMonitor(m, scale),
                                (IContainer) o);
                        }
                        else
                        {
                            m.worked(scale);
                        }
                    }
                }
            }
        }
        finally
        {
            m.done();
        }
    }

    private File configFor(final File fsLoc)
    {
        return new File(new File(fsLoc, ".git"), "config");
    }

    public void register(
        final IContainer c,
        final File gitdir,
        final String subset)
    {
        File f;
        try
        {
            f = gitdir.getCanonicalFile();
        }
        catch (IOException ioe)
        {
            f = gitdir.getAbsoluteFile();
        }
        results.add(new RepositoryMapping(c, f, subset));
    }
}
