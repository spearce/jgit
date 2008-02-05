/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *  Copyright (C) 2007  Robin Rosenberg
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.core.project;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.GitIndex.Entry;

/**
 * This class updates the index with the content of all resources tracked by git
 * in the index.
 */
public class CheckpointJob extends Job {

	private final RepositoryMapping rm;

	/**
	 * Construct a {@link CheckpointJob} for the specified resource mapping
	 *
	 * @param m
	 */
	public CheckpointJob(final RepositoryMapping m) {
		super(NLS.bind(CoreText.CheckpointJob_name, m.getContainer()
				.getFullPath()));
		setPriority(Job.LONG);
		rm = m;
	}

	protected IStatus run(IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		trace("running");
		try {
			final GitIndex index = rm.getRepository().getIndex();
			monitor.beginTask(CoreText.CheckpointJob_writing, index
					.getMembers().length);
			String prefix = rm.getSubset();
			if (prefix == null)
				prefix = "";
			else
				prefix = prefix + "/";

			Entry[] indexEntries = index.getMembers();
			for (int i = 0; i < indexEntries.length; ++i) {
				if (i % 100 == 0)
					monitor.worked(i);
				indexEntries[i].update(new File(rm.getWorkDir(),
						indexEntries[i].getName()));
			}
			index.write();
			GitProjectData.fireRepositoryChanged(rm);

		} catch (IOException ioe) {
			return Activator.error(CoreText.CheckpointJob_failed, ioe)
					.getStatus();
		} finally {
			trace("done");
			monitor.done();
		}

		return Status.OK_STATUS;
	}

	private void trace(final String m) {
		Activator.trace("(CheckpointJob " + rm.getContainer().getFullPath()
				+ ") " + m);
	}

}
