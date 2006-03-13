package org.spearce.egit.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

public class GitProvider extends RepositoryProvider {
    public String getID() {
        return getClass().getName();
    }

    public void configureProject() throws CoreException {
        GitCorePlugin.traceVerbose("GitProvider.configure: project="
                + getProject().getName());
    }

    public void deconfigure() throws CoreException {
        GitCorePlugin.traceVerbose("GitProvider.deconfigure: project="
                + getProject().getName());
    }

    public boolean canHandleLinkedResources() {
        return true;
    }
}
