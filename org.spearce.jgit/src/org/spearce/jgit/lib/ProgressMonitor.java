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

/**
 * A progress reporting interface
 */
public interface ProgressMonitor {
	/**
	 * Set information name
	 *
	 * @param message
	 */
	void setMessage(String message);

	/**
	 * @return progress message
	 */
	String getMessage();

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
	 * Request the task to be canceled
	 *
	 * @param canceled
	 */
	void setCanceled(boolean canceled);

	/**
	 * Begin a task
	 *
	 * @param message
	 * @param total
	 */
	void beginTask(String message, int total);

}
