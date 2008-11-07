/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.themes.ColorUtil;
import org.spearce.egit.ui.internal.history.SWTCommitList.SWTLane;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.revplot.AbstractPlotRenderer;
import org.spearce.jgit.revplot.PlotCommit;

class SWTPlotRenderer extends AbstractPlotRenderer<SWTLane, Color> {
	private final Color sys_blue;

	private final Color sys_black;

	private final Color sys_gray;

	private final Color sys_darkblue;

	private final Color sys_yellow;

	private final Color sys_green;

	private final Color sys_white;

	GC g;

	int cellX;

	int cellY;

	Color cellFG;

	Color cellBG;

	SWTPlotRenderer(final Display d) {
		sys_blue = d.getSystemColor(SWT.COLOR_BLUE);
		sys_black = d.getSystemColor(SWT.COLOR_BLACK);
		sys_gray = d.getSystemColor(SWT.COLOR_GRAY);
		sys_darkblue = d.getSystemColor(SWT.COLOR_DARK_BLUE);
		sys_yellow = d.getSystemColor(SWT.COLOR_YELLOW);
		sys_green = d.getSystemColor(SWT.COLOR_GREEN);
		sys_white = d.getSystemColor(SWT.COLOR_WHITE);
	}

	void paint(final Event event) {
		g = event.gc;
		cellX = event.x;
		cellY = event.y;
		cellFG = g.getForeground();
		cellBG = g.getBackground();

		final TableItem ti = (TableItem) event.item;
		paintCommit((PlotCommit<SWTLane>) ti.getData(), event.height);
	}

	protected void drawLine(final Color color, final int x1, final int y1,
			final int x2, final int y2, final int width) {
		g.setForeground(color);
		g.setLineWidth(width);
		g.drawLine(cellX + x1, cellY + y1, cellX + x2, cellY + y2);
	}

	protected void drawCommitDot(final int x, final int y, final int w,
			final int h) {
		g.setBackground(sys_blue);
		g.fillOval(cellX + x, cellY + y, w, h);
		g.setForeground(sys_darkblue);
		g.setLineWidth(2);
		g.drawOval(cellX + x + 1, cellY + y + 1, w - 2, h - 2);
		g.setForeground(sys_black);
		g.setLineWidth(1);
		g.drawOval(cellX + x, cellY + y, w, h);
	}

	protected void drawBoundaryDot(final int x, final int y, final int w,
			final int h) {
		g.setForeground(sys_gray);
		g.setBackground(cellBG);
		g.setLineWidth(1);
		g.fillOval(cellX + x, cellY + y, w, h);
		g.drawOval(cellX + x, cellY + y, w, h);
	}

	protected void drawText(final String msg, final int x, final int y) {
		final Point textsz = g.textExtent(msg);
		final int texty = (y * 2 - textsz.y) / 2;
		g.setForeground(cellFG);
		g.setBackground(cellBG);
		g.drawString(msg, cellX + x, cellY + texty, true);
	}

	@Override
	protected int drawLabel(int x, int y, Ref ref) {
		String txt;
		String name = ref.getOrigName();
		if (name.startsWith(Constants.R_HEADS)) {
			g.setBackground(sys_green);
			txt = name.substring(Constants.R_HEADS.length());
		} else if (name.startsWith(Constants.R_REMOTES)){
			g.setBackground(sys_gray);
			txt = name.substring(Constants.R_REMOTES.length());
		} else if (name.startsWith(Constants.R_TAGS)){
			g.setBackground(sys_yellow);
			txt = name.substring(Constants.R_TAGS.length());
		} else {
			// Whatever this would be
			g.setBackground(sys_white);
			if (name.startsWith(Constants.R_REFS))
				txt = name.substring(Constants.R_REFS.length());
			else
				txt = name; // HEAD and such
		}

		// Make peeled objects, i.e. via annotated tags come out in a paler color
		Color peeledColor = null;
		if (ref.getPeeledObjectId() == null || !ref.getPeeledObjectId().equals(ref.getObjectId())) {
			peeledColor = new Color(g.getDevice(), ColorUtil.blend(g.getBackground().getRGB(), sys_white.getRGB()));
			g.setBackground(peeledColor);
		}

		if (txt.length() > 12)
			txt = txt.substring(0,11) + "\u2026"; // ellipsis "…" (in UTF-8)

		Point textsz = g.stringExtent(txt);
		int arc = textsz.y/2;
		final int texty = (y * 2 - textsz.y) / 2;

		// Draw backgrounds
		g.fillRoundRectangle(x + 1, cellY + texty -1, textsz.x + 3, textsz.y + 1, arc, arc);
		g.setForeground(sys_black);
		g.drawString(txt, x + 2, cellY + texty, true);
		g.setLineWidth(2);

		// And a two color shaded border, blend with whatever background there already is
		g.setAlpha(128);
		g.setForeground(sys_gray);
		g.drawRoundRectangle(x, cellY + texty -2, textsz.x + 5, textsz.y + 3, arc, arc);
		g.setLineWidth(2);
		g.setForeground(sys_black);
		g.drawRoundRectangle(x + 1, cellY + texty -1, textsz.x + 3, textsz.y + 1, arc, arc);
		g.setAlpha(255);

		if (peeledColor != null)
			peeledColor.dispose();
		return 8 + textsz.x;
	}

	protected Color laneColor(final SWTLane myLane) {
		return myLane != null ? myLane.color : sys_black;
	}

}
