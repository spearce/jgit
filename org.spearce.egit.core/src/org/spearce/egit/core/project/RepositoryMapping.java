package org.spearce.egit.core.project;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.MergedTree;
import org.spearce.jgit.lib.RefLock;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.WriteTree;

public class RepositoryMapping
{
    public static boolean isInitialKey(final String key)
    {
        return key.endsWith(".gitdir");
    }

    private final String containerPath;

    private final String gitdirPath;

    private final String subset;

    private final String cacheref;

    private Repository db;

    private IContainer container;

    private Tree cacheTree;

    private MergedTree activeDiff;

    public RepositoryMapping(final Properties p, final String initialKey)
    {
        final int dot = initialKey.lastIndexOf('.');
        String s;

        containerPath = initialKey.substring(0, dot);
        gitdirPath = p.getProperty(initialKey);
        s = p.getProperty(containerPath + ".subset");
        subset = "".equals(s) ? null : s;
        cacheref = p.getProperty(containerPath + ".cacheref");
    }

    public RepositoryMapping(
        final IContainer mappedContainer,
        final File gitDir,
        final String subsetRoot)
    {
        final IPath cLoc = mappedContainer.getLocation()
            .removeTrailingSeparator();
        final IPath gLoc = Path.fromOSString(gitDir.getAbsolutePath())
            .removeTrailingSeparator();
        final IPath gLocParent = gLoc.removeLastSegments(1);
        String p;
        int cnt;

        container = mappedContainer;
        containerPath = container.getProjectRelativePath().toPortableString();

        if (cLoc.isPrefixOf(gLoc))
        {
            gitdirPath = gLoc.removeFirstSegments(
                gLoc.matchingFirstSegments(cLoc)).toPortableString();
        }
        else if (gLocParent.isPrefixOf(cLoc))
        {
            cnt = cLoc.segmentCount() - cLoc.matchingFirstSegments(gLocParent);
            p = "";
            while (cnt-- > 0)
            {
                p += "../";
            }
            p += gLoc.segment(gLoc.segmentCount() - 1);
            gitdirPath = p;
        }
        else
        {
            gitdirPath = gLoc.toPortableString();
        }

        subset = "".equals(subsetRoot) ? null : subsetRoot;

        p = "refs/eclipse/"
            + container.getWorkspace().getRoot().getLocation().lastSegment()
            + "/";
        IPath r = container.getFullPath();
        for (int j = 0; j < r.segmentCount(); j++)
        {
            if (j > 0)
                p += "-";
            p += r.segment(j);
        }
        cacheref = p;
    }

    public IPath getContainerPath()
    {
        return Path.fromPortableString(containerPath);
    }

    public IPath getGitDirPath()
    {
        return Path.fromPortableString(gitdirPath);
    }

    public String getSubset()
    {
        return subset;
    }

    public void clear()
    {
        db = null;
        cacheTree = null;
    }

    public Repository getRepository()
    {
        return db;
    }

    public void setRepository(final Repository r)
    {
        db = r;
        cacheTree = null;
        activeDiff = null;
    }

    public void setContainer(final IContainer c)
    {
        container = c;
    }

    public Tree getCacheTree()
    {
        return cacheTree;
    }

    public MergedTree getActiveDiff()
    {
        return activeDiff;
    }

    public void fullUpdate() throws IOException
    {
        cacheTree = mapHEADTree();

        if (container.exists())
        {
            cacheTree.accept(
                new UpdateTreeFromWorkspace(container),
                Tree.CONCURRENT_MODIFICATION);
        }
        else
        {
            cacheTree.delete();
        }

        saveCache();
    }

    public void saveCache() throws IOException
    {
        final RefLock lock;

        cacheTree.accept(new WriteTree(
            container.getLocation().toFile(),
            getRepository()), Tree.MODIFIED_ONLY);

        lock = getRepository().lockRef(cacheref);
        if (lock != null)
        {
            lock.write(cacheTree.getId());
            lock.commit();
        }

        recomputeMerge();
    }

    public void recomputeMerge() throws IOException
    {
        Tree head = mapHEADTree();

        if (cacheTree == null)
        {
            cacheTree = getRepository().mapTree(cacheref);
        }
        if (cacheTree == null)
        {
            cacheTree = new Tree(getRepository());
        }

        activeDiff = new MergedTree(new Tree[] {head, cacheTree});
    }

    public Tree mapHEADTree() throws IOException, MissingObjectException
    {
        Tree head = getRepository().mapTree(Constants.HEAD);
        if (head != null)
        {
            if (getSubset() != null)
            {
                final TreeEntry e = head.findMember(getSubset());
                e.detachParent();
                head = e instanceof Tree ? (Tree) e : null;
            }
        }
        if (head == null)
        {
            head = new Tree(getRepository());
        }
        return head;
    }

    public void store(final Properties p)
    {
        p.setProperty(containerPath + ".gitdir", gitdirPath);
        p.setProperty(containerPath + ".cacheref", cacheref);
        if (subset != null && !"".equals(subset))
        {
            p.setProperty(containerPath + ".subset", subset);
        }
    }

    public String toString()
    {
        return "RepositoryMapping["
            + containerPath
            + " -> "
            + gitdirPath
            + ", "
            + cacheref
            + "]";
    }
}
