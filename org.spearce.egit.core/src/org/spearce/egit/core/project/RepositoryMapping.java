package org.spearce.egit.core.project;

import java.io.File;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;

public class RepositoryMapping
{
    public static boolean isInitialKey(final String key)
    {
        return key.endsWith(".gitdir");
    }

    private final String containerPath;

    private final String gitdirPath;

    private final String subset;

    private String cacheName;

    private Repository db;

    private Tree cache;

    public RepositoryMapping(final Properties p, final String initialKey)
    {
        final int dot = initialKey.lastIndexOf('.');
        containerPath = initialKey.substring(0, dot);
        gitdirPath = p.getProperty(initialKey);
        final String s = p.getProperty(containerPath + ".subset");
        subset = "".equals(s) ? null : s;
        cacheName = p.getProperty(containerPath + ".cache");
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

        containerPath = mappedContainer.getProjectRelativePath()
            .toPortableString();

        if (cLoc.isPrefixOf(gLoc))
        {
            gitdirPath = gLoc.removeFirstSegments(
                gLoc.matchingFirstSegments(cLoc)).toPortableString();
        }
        else if (gLocParent.isPrefixOf(cLoc))
        {
            int cnt = cLoc.segmentCount()
                - cLoc.matchingFirstSegments(gLocParent);
            String p = "";
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

    public String getCacheName()
    {
        return cacheName;
    }

    public void setCacheName(final String n)
    {
        cacheName = n;
    }

    public void clear()
    {
        db = null;
        cache = null;
    }

    public Repository getRepository()
    {
        return db;
    }

    public void setRepository(final Repository r)
    {
        db = r;
    }

    public Tree getCacheTree()
    {
        return cache;
    }

    public void setCacheTree(final Tree t)
    {
        cache = t;
    }

    public void store(final Properties p)
    {
        p.setProperty(containerPath + ".gitdir", gitdirPath);
        p.setProperty(containerPath + ".cache", cacheName);
        if (subset != null && !"".equals(subset))
        {
            p.setProperty(containerPath + ".subset", subset);
        }
    }

    public String toString()
    {
        return "RepositoryMapping[" + containerPath + " -> " + gitdirPath + "]";
    }
}
