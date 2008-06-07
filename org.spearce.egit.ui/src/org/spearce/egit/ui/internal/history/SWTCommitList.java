/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.spearce.jgit.revplot.PlotCommitList;
import org.spearce.jgit.revplot.PlotLane;

class SWTCommitList extends PlotCommitList<SWTCommitList.SWTLane> {
	private final ArrayList<Color> allColors;

	private final LinkedList<Color> availableColors;

	SWTCommitList(final Display d) {
		allColors = new ArrayList<Color>();
		allColors.add(d.getSystemColor(SWT.COLOR_GREEN));
		allColors.add(d.getSystemColor(SWT.COLOR_BLUE));
		allColors.add(d.getSystemColor(SWT.COLOR_RED));
		allColors.add(d.getSystemColor(SWT.COLOR_MAGENTA));
		allColors.add(d.getSystemColor(SWT.COLOR_GRAY));
		allColors.add(d.getSystemColor(SWT.COLOR_DARK_YELLOW));
		allColors.add(d.getSystemColor(SWT.COLOR_DARK_CYAN));
		availableColors = new LinkedList<Color>();
		repackColors();
	}

	private void repackColors() {
		availableColors.addAll(allColors);
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
