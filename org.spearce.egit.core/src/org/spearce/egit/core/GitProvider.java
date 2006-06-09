package org.spearce.egit.core;

import java.io.IOException;

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
        try
        {
            getData();
        }
        catch (IOException ioe)
        {
            throw Activator.error(CoreText.GitProjectData_missing, ioe);
        }
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
            try
            {
                hook = new GitMoveDeleteHook(getData());
            }
            catch (IOException err)
            {
                Activator.logError(CoreText.GitProjectData_missing, err);
            }
        }
        return hook;
    }

    private synchronized GitProjectData getData() throws IOException
    {
        if (data == null)
        {
            data = GitProjectData.getDataFor(getProject());
        }
        return data;
    }
}
