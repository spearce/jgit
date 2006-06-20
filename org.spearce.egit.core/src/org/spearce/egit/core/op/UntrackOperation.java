package org.spearce.egit.core.op;

import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

public class UntrackOperation implements IWorkspaceRunnable
{
    private final Collection rsrcList;

    public UntrackOperation(final Collection rsrcs)
    {
        rsrcList = rsrcs;
    }

    public void run(IProgressMonitor m) throws CoreException
    {
        if (m == null)
        {
            m = new NullProgressMonitor();
        }

        final IdentityHashMap tomerge = new IdentityHashMap();
        m.beginTask(CoreText.UntrackOperation_adding, rsrcList.size() * 200);
        try
        {
            final Iterator i = rsrcList.iterator();
            while (i.hasNext())
            {
                final Object obj = i.next();
                if (obj instanceof IResource)
                {
                    untrack(tomerge, (IResource) obj);
                }
                m.worked(200);
            }
        }
        finally
        {
            try
            {
                final Iterator i = tomerge.keySet().iterator();
                while (i.hasNext())
                {
                    final RepositoryMapping r = (RepositoryMapping) i.next();
                    r.recomputeMerge();
                }
            }
            catch (IOException ioe)
            {
                throw Activator.error(CoreText.UntrackOperation_failed, ioe);
            }
            finally
            {
                m.done();
            }
        }
    }

    private void untrack(final Map tomerge, final IResource torm)
        throws CoreException
    {
        final GitProjectData pd = GitProjectData.getDataFor(torm.getProject());
        IResource r = torm;
        String s = null;
        RepositoryMapping m = null;

        while (r != null)
        {
            m = pd.getRepositoryMapping(r);
            if (m != null)
            {
                break;
            }

            if (s != null)
            {
                s = r.getName() + "/" + s;
            }
            else
            {
                s = r.getName();
            }

            r = r.getParent();
        }

        if (s == null || m == null || m.getCacheTree() == null)
        {
            return;
        }

        try
        {
            final Tree t = m.getCacheTree();
            final TreeEntry e = t.findMember(s);
            if (e != null)
            {
                e.delete();
                tomerge.put(m, m);
            }
        }
        catch (IOException ioe)
        {
            throw Activator.error(CoreText.UntrackOperation_failed, ioe);
        }
    }
}
