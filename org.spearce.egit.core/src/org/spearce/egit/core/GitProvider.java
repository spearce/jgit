package org.spearce.egit.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

public class GitProvider extends RepositoryProvider {
    public void configureProject() throws CoreException {
        GitCorePlugin.trace(TracingOptions.VERBOSE,
                "Configuring EGITProvider for " + getProject());
    }

    public String getID() {
        return getClass().getName();
    }

    public void deconfigure() throws CoreException {
        GitCorePlugin.trace(TracingOptions.VERBOSE,
                "Deconfiguring EGITProvider from " + getProject());
    }

    public boolean canHandleLinkedResources() {
        return true;
    }
}
