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
    private static final int OP_CREATE_REFRESH_UNITS = 10;

    private static final int OP_CREATE_REST_UNITS = 10;

    private static final int OP_CREATE_UNITS = OP_CREATE_REFRESH_UNITS
            + OP_CREATE_REST_UNITS;

    private static final int OP_REFRESH_UNITS = 75;

    private static final int OP_TOTAL_UNITS = OP_CREATE_UNITS
            + OP_REFRESH_UNITS + 5;

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

        m.beginTask(CoreText.ConnectProviderOperation_connecting,
                OP_TOTAL_UNITS);
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

                    // If we don't refresh the project directory right now we
                    // won't later know that a .git directory exists within it
                    // and we won't mark the .git directory as a team-private
                    // member. Failure to do so might allow someone to delete
                    // the .git directory without us stopping them.
                    //
                    project.refreshLocal(IResource.DEPTH_ONE,
                            new SubProgressMonitor(m, OP_CREATE_REFRESH_UNITS));

                    repos = new HashMap();
                    repos.put(project, newGitDir);

                    m.worked(OP_CREATE_REST_UNITS);
                } catch (Throwable err) {
                    throw new CoreException(new Status(IStatus.ERROR,
                            GitCorePlugin.getPluginId(), 1, err.getMessage(),
                            err));
                }
            } else {
                repos = new RepositoryFinder(project)
                        .find(new SubProgressMonitor(m, OP_CREATE_UNITS));
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
                    new SubProgressMonitor(m, OP_REFRESH_UNITS));

            RepositoryProvider.map(project, GitProvider.class.getName());
        } finally {
            m.done();
        }
    }
}
