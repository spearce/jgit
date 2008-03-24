/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.ui.internal.history;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.errors.StopWalkException;

class GenerateHistoryJob extends Job {
	private final GitHistoryPage page;

	private final SWTCommitList allCommits;

	GenerateHistoryJob(final GitHistoryPage ghp, final SWTCommitList list) {
		super(UIText.HistoryPage_refreshJob);
		page = ghp;
		allCommits = list;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		try {
			boolean cancel = false;
			try {
				allCommits.fillTo(this, monitor, Integer.MAX_VALUE);
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, Activator.getPluginId(),
						"Cannot compute Git history.", e);
			} catch (StopWalkException swe) {
				cancel = true;
			}

			if (cancel || monitor.isCanceled())
				return Status.CANCEL_STATUS;
			updateUI();
		} finally {
			monitor.done();
		}
		return status;
	}

	void updateUI() {
		final SWTCommit[] asArray = new SWTCommit[allCommits.size()];
		allCommits.toArray(asArray);
		page.showCommitList(this, allCommits, asArray);
	}
}