package org.spearce.egit.core.op;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryFinder;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.FullRepository;

public class ConnectProviderOperation implements IWorkspaceRunnable
{
    private final IProject project;

    private final File newGitDir;

    public ConnectProviderOperation(final IProject proj, final File newdir)
    {
        project = proj;
        newGitDir = newdir;
    }

    public void run(IProgressMonitor m) throws CoreException
    {
        if (m == null)
        {
            m = new NullProgressMonitor();
        }

        m.beginTask(CoreText.ConnectProviderOperation_connecting, 100);
        try
        {
            final Collection repos = new ArrayList();

            if (newGitDir != null)
            {
                try
                {
                    final FullRepository db;

                    m.subTask(CoreText.ConnectProviderOperation_creating);
                    Activator.trace("Creating repository " + newGitDir);

                    db = new FullRepository(newGitDir);
                    db.create();
                    repos.add(new RepositoryMapping(project, db));
                    db.close();

                    // If we don't refresh the project directory right now we
                    // won't later know that a .git directory exists within it
                    // and we won't mark the .git directory as a team-private
                    // member. Failure to do so might allow someone to delete
                    // the .git directory without us stopping them.
                    //
                    project.refreshLocal(
                        IResource.DEPTH_ONE,
                        new SubProgressMonitor(m, 10));

                    m.worked(10);
                }
                catch (Throwable err)
                {
                    throw Activator.error(
                        CoreText.ConnectProviderOperation_creating,
                        err);
                }
            }
            else
            {
                Activator.trace("Finding existing repositories.");
                repos.addAll(new RepositoryFinder(project)
                    .find(new SubProgressMonitor(m, 20)));
            }

            m.subTask(CoreText.ConnectProviderOperation_recordingMapping);
            final GitProjectData projectData = new GitProjectData(project);
            projectData.setRepositoryMappings(repos);
            projectData.store();
            projectData.markTeamPrivateResources();
            projectData.cache();

            project.refreshLocal(
                IResource.DEPTH_INFINITE,
                new SubProgressMonitor(m, 75));

            try
            {
                RepositoryProvider.map(project, GitProvider.class.getName());
            }
            catch (CoreException ce)
            {
                GitProjectData.deleteDataFor(project);
                throw ce;
            }
            catch (RuntimeException ce)
            {
                GitProjectData.deleteDataFor(project);
                throw ce;
            }
        }
        finally
        {
            m.done();
        }
    }
}
