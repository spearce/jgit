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
 * A simple progress reporter printing on stderr
 */
public class TextProgressMonitor implements ProgressMonitor {
	private boolean output;

	private long taskBeganAt;

	private String msg;

	private int lastWorked;

	private int totalWork;

	/** Initialize a new progress monitor. */
	public TextProgressMonitor() {
		taskBeganAt = System.currentTimeMillis();
	}

	public void start(final int totalTasks) {
		// Ignore the number of tasks.
		taskBeganAt = System.currentTimeMillis();
	}

	public void beginTask(final String title, final int total) {
		endTask();
		msg = title;
		lastWorked = 0;
		totalWork = total;
	}

	public void update(final int completed) {
		if (msg == null)
			return;

		final int cmp = lastWorked + completed;
		if (!output && System.currentTimeMillis() - taskBeganAt < 500)
			return;
		if (totalWork == UNKNOWN) {
			if (cmp % 100 == 0) {
				display(cmp);
				System.err.flush();
			}
		} else {
			if ((cmp * 100 / totalWork) != (lastWorked * 100) / totalWork) {
				display(cmp);
				System.err.flush();
			}
		}
		lastWorked = cmp;
		output = true;
	}

	private void display(final int cmp) {
		final StringBuilder m = new StringBuilder();
		m.append('\r');
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

		System.err.print(m);
	}

	public boolean isCancelled() {
		return false;
	}

	public void endTask() {
		if (output) {
			if (totalWork != UNKNOWN)
				display(totalWork);
			System.err.println();
		}
		output = false;
		msg = null;
	}
}
