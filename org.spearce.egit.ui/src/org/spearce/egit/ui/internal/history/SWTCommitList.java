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
import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.errors.StopWalkException;
import org.spearce.jgit.revplot.PlotCommitList;
import org.spearce.jgit.revplot.PlotLane;

class SWTCommitList extends PlotCommitList<SWTCommitList.SWTLane> {
	private GenerateHistoryJob job;

	private IProgressMonitor monitor;

	private final ArrayList<Color> allColors;

	private final LinkedList<Color> availableColors;

	SWTCommitList(final Display d) {
		allColors = new ArrayList<Color>();
		allColors.add(d.getSystemColor(SWT.COLOR_BLACK));
		allColors.add(d.getSystemColor(SWT.COLOR_BLUE));
		allColors.add(d.getSystemColor(SWT.COLOR_CYAN));
		allColors.add(d.getSystemColor(SWT.COLOR_DARK_BLUE));
		allColors.add(d.getSystemColor(SWT.COLOR_DARK_CYAN));
		allColors.add(d.getSystemColor(SWT.COLOR_DARK_GREEN));
		allColors.add(d.getSystemColor(SWT.COLOR_DARK_MAGENTA));
		allColors.add(d.getSystemColor(SWT.COLOR_DARK_RED));
		allColors.add(d.getSystemColor(SWT.COLOR_DARK_YELLOW));
		allColors.add(d.getSystemColor(SWT.COLOR_GRAY));
		allColors.add(d.getSystemColor(SWT.COLOR_GREEN));
		allColors.add(d.getSystemColor(SWT.COLOR_MAGENTA));
		allColors.add(d.getSystemColor(SWT.COLOR_RED));

		availableColors = new LinkedList<Color>();
		repackColors();
	}

	private void repackColors() {
		availableColors.addAll(allColors);
	}

	void fillTo(final GenerateHistoryJob j, final IProgressMonitor m,
			final int max) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		job = j;
		monitor = m;
		try {
			fillTo(max);
		} finally {
			job = j;
			monitor = null;
		}
	}

	@Override
	protected void filledBatch() {
		super.filledBatch();
		if (monitor.isCanceled())
			throw StopWalkException.INSTANCE;
		job.updateUI();
	}

	@Override
	protected SWTLane createLane() {
		final SWTLane lane = new SWTLane();
		if (availableColors.isEmpty())
			repackColors();
		lane.color = availableColors.removeFirst();
		return lane;
	}

	@Override
	protected void recycleLane(final SWTLane lane) {
		availableColors.add(lane.color);
	}

	static class SWTLane extends PlotLane {
		Color color;
	}
}
