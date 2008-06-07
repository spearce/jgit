/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;

class GenerateHistoryJob extends Job {
	private static final int BATCH_SIZE = 256;

	private final GitHistoryPage page;

	private final SWTCommitList allCommits;

	private int lastUpdateCnt;

	private long lastUpdateAt;

	GenerateHistoryJob(final GitHistoryPage ghp, final SWTCommitList list) {
		super(UIText.HistoryPage_refreshJob);
		page = ghp;
		allCommits = list;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		try {
			try {
				for (;;) {
					final int oldsz = allCommits.size();
					allCommits.fillTo(oldsz + BATCH_SIZE - 1);
					if (monitor.isCanceled() || oldsz == allCommits.size())
						break;

					final long now = System.currentTimeMillis();
					if (now - lastUpdateAt < 2000 && lastUpdateCnt > 0)
						continue;
					updateUI();
					lastUpdateAt = now;
				}
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, Activator.getPluginId(),
						"Cannot compute Git history.", e);
			}

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			updateUI();
		} finally {
			monitor.done();
		}
		return status;
	}

	void updateUI() {
		if (allCommits.size() == lastUpdateCnt)
			return;

		final SWTCommit[] asArray = new SWTCommit[allCommits.size()];
		allCommits.toArray(asArray);
		page.showCommitList(this, allCommits, asArray);
		lastUpdateCnt = allCommits.size();
	}
}
