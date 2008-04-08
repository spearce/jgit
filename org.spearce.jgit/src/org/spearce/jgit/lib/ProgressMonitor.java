/*
 *  Copyright (C) 2007  Robin Rosenberg
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
package org.spearce.jgit.lib;

/** A progress reporting interface. */
public interface ProgressMonitor {
	/** Constant indicating the total work units cannot be predicted. */
	public static final int UNKNOWN = 0;

	/**
	 * Advise the monitor of the total number of subtasks.
	 * <p>
	 * This should be invoked at most once per progress monitor interface.
	 * 
	 * @param totalTasks
	 *            the total number of tasks the caller will need to complete
	 *            their processing.
	 */
	void start(int totalTasks);

	/**
	 * Begin processing a single task.
	 * 
	 * @param title
	 *            title to describe the task. Callers should publish these as
	 *            stable string constants that implementations could match
	 *            against for translation support.
	 * @param totalWork
	 *            total number of work units the application will perform;
	 *            {@link #UNKNOWN} if it cannot be predicted in advance.
	 */
	void beginTask(String title, int totalWork);

	/**
	 * Denote that some work units have been completed.
	 * <p>
	 * This is an incremental update; if invoked once per work unit the correct
	 * value for our argument is <code>1</code>, to indicate a single unit of
	 * work has been finished by the caller.
	 * 
	 * @param completed
	 *            the number of work units completed since the last call.
	 */
	void update(int completed);

	/** Finish the current task, so the next can begin. */
	void endTask();

	/**
	 * Check for user task cancellation.
	 * 
	 * @return true if the user asked the process to stop working.
	 */
	boolean isCancelled();
}
