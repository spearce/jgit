package org.spearce.egit.core.op;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.GitCorePlugin;
import org.spearce.egit.core.GitProvider;
import org.spearce.jgit.lib.Repository;

public class ConnectProviderOperation implements IWorkspaceRunnable {
    private final IProject project;

    private final File gitdir;

    private final boolean createRepository;

    public ConnectProviderOperation(final IProject proj, final File repo,
            final boolean create) {
        project = proj;
        gitdir = repo;
        createRepository = create;
    }

    public void run(IProgressMonitor monitor) throws CoreException {
        if (createRepository) {
            if (monitor == null) {
                monitor = new NullProgressMonitor();
            }

            monitor.beginTask(CoreText.ConnectProviderOperation_creating, 100);
            try {
                try {
                    final Repository db = new Repository(gitdir);
                    db.initialize();
                    monitor.worked(25);
                } catch (Throwable err) {
                    throw new CoreException(new Status(IStatus.ERROR,
                            GitCorePlugin.getPluginId(), 1, err.getMessage(),
                            err));
                }

                project.refreshLocal(IResource.DEPTH_INFINITE,
                        new SubProgressMonitor(monitor, 75));
            } finally {
                monitor.done();
            }
        }

        RepositoryProvider.map(project, GitProvider.class.getName());
    }
}
