package org.spearce.egit.core.project;

import java.io.File;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.spearce.jgit.lib.Repository;

public class RepositoryMapping
{
    public static boolean isKey(final String key)
    {
        return key.endsWith(".gitdir");
    }

    private String containerPath;

    private String gitdirPath;

    private String subset;

    public RepositoryMapping(final Properties p, final String key)
    {
        final int dot = key.lastIndexOf('.');
        containerPath = key.substring(0, dot);
        gitdirPath = p.getProperty(key);
        subset = p.getProperty(containerPath + ".subset");
    }

    public RepositoryMapping(final IContainer c, final Repository r)
    {
        this(c, r.getDirectory(), r.getSubsetPath());
    }

    public RepositoryMapping(final IContainer c, final File g, final String s)
    {
        final IPath cLoc = c.getLocation().removeTrailingSeparator();
        final IPath gLoc = Path.fromOSString(g.getAbsolutePath())
            .removeTrailingSeparator();
        final IPath gLocParent = gLoc.removeLastSegments(1);

        containerPath = c.getProjectRelativePath().toPortableString();
        if (cLoc.isPrefixOf(gLoc))
        {
            gitdirPath = gLoc.removeFirstSegments(
                gLoc.matchingFirstSegments(cLoc)).toPortableString();
        }
        else if (gLocParent.isPrefixOf(cLoc))
        {
            int cnt = cLoc.segmentCount()
                - cLoc.matchingFirstSegments(gLocParent);
            gitdirPath = "";
            while (cnt-- > 0)
            {
                gitdirPath += "../";
            }
            gitdirPath += gLoc.segment(gLoc.segmentCount() - 1);
        }
        else
        {
            gitdirPath = gLoc.toPortableString();
        }
        subset = "".equals(s) ? null : s;
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

    public void store(final Properties p)
    {
        p.setProperty(containerPath + ".gitdir", gitdirPath);
        if (subset != null && !"".equals(subset))
        {
            p.setProperty(containerPath + ".subset", subset);
        }
    }
}
