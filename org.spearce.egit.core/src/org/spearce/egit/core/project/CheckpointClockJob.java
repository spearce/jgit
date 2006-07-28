/*
 *    Copyright 2006 Shawn Pearce <spearce@spearce.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
