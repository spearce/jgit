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
 * Base class for progress monitors. This class tracks progress
 * but does not report it anywhere;
 */
public abstract class AbstractProgressMonitor implements ProgressMonitor {

	private int total;
	private int worked;
	private String task;
	private boolean done;

	public void done() {
		done = true;
		worked = total;
		report();
	}

	public boolean isDone() {
		return done;
	}

	public boolean isCancelled() {
		return false;
	}

	public void setCancelled(final boolean cancelled) {
		// empty
	}

	public void setTask(final String task) {
		this.task = task;
		report();
	}

	public void worked(final int amount) {
		worked += amount;
		report();
	}

	public void beginTask(final String task, final int total) {
		this.task = task;
		this.total = total;
		report();
	}

	/**
	 * Report progress
	 */
	abstract protected void report();

	public String getTask() {
		return task;
	}

	public int getWorked() {
		return worked;
	}

	public void setTotalWork(final int work) {
		total = work;
	}

	public int getTotal() {
		return total;
	}
}
