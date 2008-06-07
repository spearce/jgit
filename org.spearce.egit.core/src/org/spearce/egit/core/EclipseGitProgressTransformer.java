/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.spearce.jgit.lib.ProgressMonitor;

/** Create a new Git to Eclipse progress monitor. */
public class EclipseGitProgressTransformer implements ProgressMonitor {
	private final IProgressMonitor root;

	private IProgressMonitor task;

	private String msg;

	private int lastWorked;

	private int totalWork;

	/**
	 * Create a new progress monitor.
	 * 
	 * @param eclipseMonitor
	 *            the Eclipse monitor we update.
	 */
	public EclipseGitProgressTransformer(final IProgressMonitor eclipseMonitor) {
		root = eclipseMonitor;
	}

	public void start(final int totalTasks) {
		root.beginTask("", totalTasks * 1000);
	}

	public void beginTask(final String name, final int total) {
		endTask();
		msg = name;
		lastWorked = 0;
		totalWork = total;
		task = new SubProgressMonitor(root, 1000);
		if (totalWork == UNKNOWN)
			task.beginTask("", IProgressMonitor.UNKNOWN);
		else
			task.beginTask("", totalWork);
		task.subTask(msg);
	}

	public void update(final int work) {
		if (task == null)
			return;

		final int cmp = lastWorked + work;
		if (lastWorked == UNKNOWN && cmp > 0) {
			task.subTask(msg + ", " + cmp);
		} else if (totalWork <= 0) {
			// Do nothing to update the task.
		} else if (cmp * 100 / totalWork != lastWorked * 100 / totalWork) {
			final StringBuilder m = new StringBuilder();
			m.append(msg);
			m.append(": ");
			while (m.length() < 25)
				m.append(' ');

			if (totalWork == UNKNOWN) {
				m.append(cmp);
			} else {
				final String twstr = String.valueOf(totalWork);
				String cmpstr = String.valueOf(cmp);
				while (cmpstr.length() < twstr.length())
					cmpstr = " " + cmpstr;
				final int pcnt = (cmp * 100 / totalWork);
				if (pcnt < 100)
					m.append(' ');
				if (pcnt < 10)
					m.append(' ');
				m.append(pcnt);
				m.append("% (");
				m.append(cmpstr);
				m.append("/");
				m.append(twstr);
				m.append(")");
			}
			task.subTask(m.toString());
		}
		lastWorked = cmp;
		task.worked(work);
	}

	public void endTask() {
		if (task != null) {
			try {
				task.done();
			} finally {
				task = null;
			}
		}
	}

	public boolean isCancelled() {
		if (task != null)
			return task.isCanceled();
		return root.isCanceled();
	}
}
