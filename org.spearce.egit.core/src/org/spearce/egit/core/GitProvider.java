package org.spearce.egit.core;

import java.io.IOException;

import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;

public class GitProvider extends RepositoryProvider {
    private GitProjectData data;

    private GitMoveDeleteHook hook;

    public String getID() {
        return getClass().getName();
    }

    public void configureProject() throws CoreException {
        try {
            getData();
        } catch (IOException ioe) {
            throw new CoreException(new Status(IStatus.ERROR, GitCorePlugin
                    .getPluginId(), 1, ioe.getMessage(), ioe));
        }
    }

    public void deconfigure() throws CoreException {
        GitProjectData.deleteDataFor(getProject());
    }

    public boolean canHandleLinkedResources() {
        return true;
    }

    public synchronized IMoveDeleteHook getMoveDeleteHook() {
        if (hook == null) {
            try {
                hook = new GitMoveDeleteHook(getData());
            } catch (IOException err) {
                final IllegalStateException e;
                e = new IllegalStateException("GitProjectData missing.");
                e.initCause(err);
                throw e;
            }
        }
        return hook;
    }

    private synchronized GitProjectData getData() throws IOException {
        if (data == null) {
            data = GitProjectData.getInstance(getProject());
        }
        return data;
    }
}
