/*
 *  Copyright (C) 2008  Roger C. Soares
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
package org.spearce.egit.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.spearce.jgit.lib.ProgressMonitor;

/** Create a new Git to Eclipse progress monitor. */
public class EclipseGitProgressTransformer implements ProgressMonitor {
	private final IProgressMonitor root;

	private IProgressMonitor task;

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

	public void beginTask(final String name, final int totalWork) {
		endTask();
		task = new SubProgressMonitor(root, 1000);
		if (totalWork == UNKNOWN)
			task.beginTask(name, IProgressMonitor.UNKNOWN);
		else
			task.beginTask(name, totalWork);
	}

	public void update(final int work) {
		if (task != null)
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
