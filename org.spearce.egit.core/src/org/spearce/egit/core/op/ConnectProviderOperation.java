package org.spearce.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import org.spearce.egit.core.GitProjectData;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.RepositoryFinder;
import org.spearce.jgit.lib.FullRepository;

public class ConnectProviderOperation implements IWorkspaceRunnable {
    private final IProject project;

    private final File newGitDir;

    private final boolean createRepository;

    public ConnectProviderOperation(final IProject proj, final boolean create,
            final File newdir) {
        project = proj;
        createRepository = create;
        newGitDir = newdir;
    }

    public void run(IProgressMonitor m) throws CoreException {
        if (m == null) {
            m = new NullProgressMonitor();
        }

        m.beginTask(CoreText.ConnectProviderOperation_connecting, 100);
        try {
            final Map repos;

            if (createRepository) {
                try {
                    final FullRepository db;

                    m.subTask(CoreText.ConnectProviderOperation_creating);
                    GitCorePlugin.traceVerbose("Creating new GIT repository: "
                            + newGitDir);

                    db = new FullRepository(newGitDir);
                    db.create();
                    db.close();

                    // If we don't refresh the project directory right now we
                    // won't later know that a .git directory exists within it
                    // and we won't mark the .git directory as a team-private
                    // member. Failure to do so might allow someone to delete
                    // the .git directory without us stopping them.
                    //
                    project.refreshLocal(IResource.DEPTH_ONE,
                            new SubProgressMonitor(m, 10));

                    repos = new HashMap();
                    repos.put(project, newGitDir);

                    m.worked(10);
                } catch (Throwable err) {
                    throw new CoreException(new Status(IStatus.ERROR,
                            GitCorePlugin.getPluginId(), 1, err.getMessage(),
                            err));
                }
            } else {
                repos = new RepositoryFinder(project)
                        .find(new SubProgressMonitor(m, 20));
            }

            m.subTask(CoreText.ConnectProviderOperation_recordingMapping);

            GitProjectData projectData = new GitProjectData();
            try {
                projectData.setProject(project);
                projectData.setRepositoryMappings(repos);
                projectData.save();
            } catch (IOException err) {
                throw new CoreException(new Status(IStatus.ERROR, GitCorePlugin
                        .getPluginId(), 1, err.getMessage(), err));
            }
            projectData.markTeamPrivateResources();

            project.refreshLocal(IResource.DEPTH_INFINITE,
                    new SubProgressMonitor(m, 75));

            RepositoryProvider.map(project, GitProvider.class.getName());
        } finally {
            m.done();
        }
    }
}
