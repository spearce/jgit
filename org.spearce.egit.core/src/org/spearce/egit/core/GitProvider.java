package org.spearce.egit.core;

import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.project.GitProjectData;

public class GitProvider extends RepositoryProvider
{
    private GitProjectData data;

    private GitMoveDeleteHook hook;

    public String getID()
    {
        return getClass().getName();
    }

    public void configureProject() throws CoreException
    {
        getData().markTeamPrivateResources();
        getData().rebuildCache();
    }

    public void deconfigure() throws CoreException
    {
        GitProjectData.deleteDataFor(getProject());
    }

    public boolean canHandleLinkedResources()
    {
        return true;
    }

    public synchronized IMoveDeleteHook getMoveDeleteHook()
    {
        if (hook == null)
        {
            hook = new GitMoveDeleteHook(getData());
        }
        return hook;
    }

    private synchronized GitProjectData getData()
    {
        if (data == null)
        {
            data = GitProjectData.getDataFor(getProject());
        }
        return data;
    }
}
