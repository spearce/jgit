package org.spearce.egit.core.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.GitProvider;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.MissingObjectException;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

public class GitProjectData
{
    private static final Map projectDataCache = new HashMap();

    private static final Map repositoryCache = new HashMap();

    private static final IResourceChangeListener uncacher = new IResourceChangeListener()
    {
        public void resourceChanged(final IResourceChangeEvent event)
        {
            final IResource r = event.getResource();
            if (r instanceof IProject)
            {
                uncacheDataFor((IProject) r);
            }
        }
    };

    private static void trace(final String m)
    {
        Activator.trace("(GitProjectData) " + m);
    }

    public synchronized static GitProjectData getDataFor(final IProject p)
    {
        try
        {
            GitProjectData d = (GitProjectData) projectDataCache.get(p);
            if (d == null
                && RepositoryProvider.getProvider(p) instanceof GitProvider)
            {
                d = loadDataFor(p);
                final int evt = IResourceChangeEvent.PRE_CLOSE
                    | IResourceChangeEvent.PRE_DELETE;
                p.getWorkspace().addResourceChangeListener(uncacher, evt);
                cacheDataFor(p, d);
                trace("getDataFor(" + p.getName() + ")");
            }
            return d;
        }
        catch (IOException err)
        {
            Activator.logError(CoreText.GitProjectData_missing, err);
            return null;
        }
    }

    private synchronized static void cacheDataFor(
        final IProject p,
        final GitProjectData d)
    {
        projectDataCache.put(p, d);
    }

    private synchronized static void uncacheDataFor(final IProject p)
    {
        if (projectDataCache.remove(p) != null)
        {
            trace("uncacheDataFor(" + p.getName() + ")");
        }
    }

    public static void deleteDataFor(final IProject p)
    {
        final File dir = fileFor(p).getParentFile();
        final File[] todel = dir.listFiles();
        if (todel != null)
        {
            for (int k = 0; k < todel.length; k++)
            {
                if (todel[k].isFile())
                {
                    todel[k].delete();
                }
            }
        }
        dir.delete();
        trace("deleteDataFor(" + p.getName() + ")");
        uncacheDataFor(p);
    }

    public static GitProjectData loadDataFor(final IProject p)
        throws IOException
    {
        final GitProjectData d = new GitProjectData(p);
        d.load();
        return d;
    }

    public synchronized static Repository getRepository(final File gitDir)
        throws IOException
    {
        final Iterator i = repositoryCache.entrySet().iterator();
        while (i.hasNext())
        {
            final Map.Entry e = (Map.Entry) i.next();
            if (((Reference) e.getValue()).get() == null)
            {
                i.remove();
            }
        }

        final Reference r = (Reference) repositoryCache.get(gitDir);
        Repository d = r != null ? (Repository) r.get() : null;
        if (d == null)
        {
            d = new Repository(gitDir);
            repositoryCache.put(gitDir, new WeakReference(d));
        }
        return d;
    }

    private static File fileFor(final IProject p)
    {
        return new File(
            p.getWorkingLocation(Activator.getPluginId()).toFile(),
            "GitProjectData.properties");
    }

    private final IProject project;

    private final Collection mappings;

    private final Map c2mapping;

    private final Set protectedResources;

    public GitProjectData(final IProject p)
    {
        project = p;
        mappings = new ArrayList();
        c2mapping = new HashMap();
        protectedResources = new HashSet();
    }

    public IProject getProject()
    {
        return project;
    }

    public void setRepositoryMappings(final Collection newMappings)
    {
        mappings.clear();
        mappings.addAll(newMappings);
        remapAll();
    }

    public void markTeamPrivateResources() throws CoreException
    {
        final Iterator i = c2mapping.entrySet().iterator();
        while (i.hasNext())
        {
            final Map.Entry e = (Map.Entry) i.next();
            final IContainer c = (IContainer) e.getKey();
            final IResource dotGit = c.findMember(".git");
            if (dotGit != null)
            {
                try
                {
                    final Repository r = ((RepositoryMapping) e.getValue())
                        .getRepository();
                    final File dotGitDir = dotGit.getLocation().toFile()
                        .getCanonicalFile();
                    if (dotGitDir.equals(r.getDirectory()))
                    {
                        trace("teamPrivate " + dotGit);
                        dotGit.setTeamPrivateMember(true);
                    }
                }
                catch (IOException err)
                {
                    throw Activator.error(CoreText.Error_CanonicalFile, err);
                }
            }
        }
    }

    public void rebuildCache() throws CoreException
    {
        final Iterator i = mappings.iterator();
        while (i.hasNext())
        {
            rebuildCache((RepositoryMapping) i.next());
        }
    }

    private void rebuildCache(final RepositoryMapping m) throws CoreException
    {
        if (m.getRepository() != null)
        {
            try
            {
                final ObjectId head = m.getRepository().resolve("HEAD");
                Tree t;

                if (head != null)
                {
                    trace("rebuildCache("
                        + m.getContainerPath()
                        + ") mapTree "
                        + head);

                    t = m.getRepository().mapTree(head);
                    if (t == null)
                    {
                        throw new MissingObjectException(Constants.TYPE_TREE
                            + ", "
                            + Constants.TYPE_COMMIT
                            + " or "
                            + Constants.TYPE_TAG, head);
                    }
                    if (m.getSubset() != null)
                    {
                        final TreeEntry e = t.findMember(m.getSubset());
                        if (!(e instanceof Tree))
                        {
                            throw new IOException("No such tree: "
                                + m.getSubset());
                        }
                        t = (Tree) e;
                    }
                }
                else
                {
                    t = new Tree(m.getRepository());
                }

                trace("rebuildCache("
                    + m.getContainerPath()
                    + ") "
                    + t.getId()
                    + " "
                    + t.getFullName());
                t.detachParent();
                m.setCacheTree(t);
            }
            catch (IOException ioe)
            {
                throw Activator.error(
                    CoreText.GitProjectData_cannotReadHEAD,
                    ioe);
            }
        }
    }

    public boolean isProtected(final IResource f)
    {
        return protectedResources.contains(f);
    }

    public RepositoryMapping getRepositoryMapping(
        IResource r,
        final boolean walk)
    {
        while (r != null)
        {
            final RepositoryMapping m = (RepositoryMapping) c2mapping.get(r);
            if (m != null)
            {
                return m;
            }

            if (!walk)
            {
                return null;
            }

            r = r.getParent();
        }
        return null;
    }

    public TreeEntry getTreeEntry(IResource r) throws IOException
    {
        String s = null;
        Tree t = null;

        while (r != null)
        {
            // t = (Tree) c2tree.get(r);
            if (t != null)
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

        if (s != null && t != null)
        {
            return t.findMember(s);
        }

        return t;
    }

    public void store() throws CoreException
    {
        final File dat = fileFor(getProject());
        final File tmp;
        boolean ok = false;

        try
        {
            trace("save " + dat);
            tmp = File.createTempFile("gpd_", ".prop", dat.getParentFile());
            final FileOutputStream o = new FileOutputStream(tmp);
            try
            {
                final Properties p = new Properties();
                final Iterator i = mappings.iterator();
                while (i.hasNext())
                {
                    ((RepositoryMapping) i.next()).store(p);
                }
                p.store(o, "GitProjectData");
                ok = true;
            }
            finally
            {
                o.close();
                if (!ok)
                {
                    tmp.delete();
                }
            }
        }
        catch (IOException ioe)
        {
            throw Activator.error(CoreText.bind(
                CoreText.GitProjectData_saveFailed,
                dat), ioe);
        }

        dat.delete();
        if (!tmp.renameTo(dat))
        {
            tmp.delete();
            throw Activator.error(CoreText.bind(
                CoreText.GitProjectData_saveFailed,
                dat), null);
        }
    }

    private void load() throws IOException
    {
        final File dat = fileFor(getProject());
        trace("load " + dat);

        final FileInputStream o = new FileInputStream(dat);
        try
        {
            final Properties p = new Properties();
            p.load(o);

            mappings.clear();
            final Iterator keyItr = p.keySet().iterator();
            while (keyItr.hasNext())
            {
                final String key = keyItr.next().toString();
                if (RepositoryMapping.isInitialKey(key))
                {
                    mappings.add(new RepositoryMapping(p, key));
                }
            }
        }
        finally
        {
            o.close();
        }
        remapAll();
    }

    private void remapAll()
    {
        protectedResources.clear();
        final Iterator i = mappings.iterator();
        while (i.hasNext())
        {
            map((RepositoryMapping) i.next());
        }
    }

    private void map(final RepositoryMapping m)
    {
        final IResource c;
        final File git;
        final IResource dotGit;

        m.clear();
        c = getProject().findMember(m.getContainerPath());
        if (c == null || !(c instanceof IContainer))
        {
            Activator.logError(
                CoreText.GitProjectData_mappedResourceGone,
                new FileNotFoundException(m.getContainerPath().toString()));
            return;
        }

        git = c.getLocation().append(m.getGitDirPath()).toFile();
        if (!git.isDirectory() || !new File(git, "config").isFile())
        {
            Activator.logError(
                CoreText.GitProjectData_mappedResourceGone,
                new FileNotFoundException(m.getContainerPath().toString()));
            return;
        }

        try
        {
            m.setRepository(getRepository(git));
            trace("map " + c + " -> " + m.getRepository());
        }
        catch (IOException ioe)
        {
            Activator.logError(
                CoreText.GitProjectData_mappedResourceGone,
                new FileNotFoundException(m.getContainerPath().toString()));
            return;

        }

        dotGit = ((IContainer) c).findMember(".git");
        if (dotGit != null && dotGit.getLocation().toFile().equals(git))
        {
            protect(dotGit);
        }
    }

    private void protect(IResource c)
    {
        while (c != null && !c.equals(getProject()))
        {
            trace("protect " + c);
            protectedResources.add(c);
            c = c.getParent();
        }
    }
}
