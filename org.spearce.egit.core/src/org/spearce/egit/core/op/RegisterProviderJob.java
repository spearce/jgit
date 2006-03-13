package org.spearce.egit.core.op;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.GitProvider;

public class RegisterProviderJob extends Job {
    private final IProject project;

    public RegisterProviderJob(final IProject proj) {
        super(CoreText.RegisterProviderJob_name);
        project = proj;
    }

    protected IStatus run(IProgressMonitor monitor) {
        try {
            RepositoryProvider.map(project, GitProvider.class.getName());
            return Status.OK_STATUS;
        } catch (TeamException e) {
            return e.getStatus();
        }
    }
}
