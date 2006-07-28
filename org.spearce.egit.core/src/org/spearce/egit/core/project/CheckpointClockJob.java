package org.spearce.egit.core.project;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class CheckpointClockJob extends Job
{
    public CheckpointClockJob()
    {
        super("BLAH");
        setPriority(LONG);
        setSystem(true);
        setRule(null);
    }

    protected IStatus run(final IProgressMonitor monitor)
    {
        GitProjectData.checkpointAllProjects();
        schedule(15 * 1000);
        return Status.OK_STATUS;
    }
}
