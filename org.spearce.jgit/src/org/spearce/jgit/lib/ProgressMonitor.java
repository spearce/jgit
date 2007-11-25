/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.spearce.jgit.lib;

/**
 * A progress monitor. A ripoff of IProgressMonitor.
 */
public interface ProgressMonitor {
	/**
	 * Set task name
	 *
	 * @param message
	 */
	void setTask(String message);

	/**
	 * @return taskname
	 */
	String getTask();

	/**
	 * Set the total expected amount of work
	 *
	 * @param work
	 */
	void setTotalWork(int work);

	/**
	 * @return amount worked so far
	 */
	int getWorked();

	/**
	 * @param work
	 */
	void worked(int work);

	/**
	 * @return total expected amount of work
	 */
	int getTotal();

	/**
	 * Indicate the task is completed.
	 */
	void done();

	/**
	 * @return true if done.
	 */
	boolean isDone();

	/**
	 * @return true if cancel has been requested.
	 */
	boolean isCancelled();

	/**
	 * Request the task to be cancelled
	 *
	 * @param cancelled
	 */
	void setCancelled(boolean cancelled);

	/**
	 * Begin a task
	 *
	 * @param task
	 * @param total
	 */
	void beginTask(String task, int total);

}
