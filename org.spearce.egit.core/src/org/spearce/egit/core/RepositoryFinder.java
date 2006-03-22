package org.spearce.egit.core;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

public class RepositoryFinder {
    private final IProject proj;

    private final Map results;

    public RepositoryFinder(final IProject p) {
        proj = p;
        results = new HashMap();
    }

    public Map find(IProgressMonitor m) throws CoreException {
        if (m == null) {
            m = new NullProgressMonitor();
        }
        find(m, proj);
        return Collections.unmodifiableMap(results);
    }

    private void find(final IProgressMonitor m, final IContainer c)
            throws CoreException {
        final IPath loc = c.getLocation();

        m.beginTask("", 101);
        m.subTask(CoreText.RepositoryFinder_finding);
        try {
            if (loc != null) {
                final File fsLoc = loc.toFile();
                final File ownCfg = configFor(fsLoc);
                final IResource[] children;

                if (ownCfg.isFile()) {
                    register(c, ownCfg.getParentFile());
                } else if (c.isLinked() || c instanceof IProject) {
                    File p = fsLoc.getParentFile();
                    while (p != null) {
                        final File pCfg = configFor(p);
                        if (pCfg.isFile()) {
                            register(c, pCfg.getParentFile());
                            break;
                        }
                        p = p.getParentFile();
                    }
                }
                m.worked(1);

                children = c.members();
                if (children != null && children.length > 0) {
                    final int scale = 100 / children.length;
                    for (int k = 0; k < children.length; k++) {
                        final IResource o = children[k];
                        if (o instanceof IContainer
                                && !o.getName().equals(".git")) {
                            find(new SubProgressMonitor(m, scale),
                                    (IContainer) o);
                        } else {
                            m.worked(scale);
                        }
                    }
                }
            }
        } finally {
            m.done();
        }
    }

    private File configFor(final File fsLoc) {
        return new File(new File(fsLoc, ".git"), "config");
    }

    public void register(final IContainer c, final File gitdir) {
        File f;
        try {
            f = gitdir.getCanonicalFile();
        } catch (IOException ioe) {
            f = gitdir.getParentFile();
        }
        results.put(c, f);
    }
}
