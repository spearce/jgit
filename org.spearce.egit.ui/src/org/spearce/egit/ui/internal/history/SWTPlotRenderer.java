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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.spearce.egit.ui.internal.history.SWTCommitList.SWTLane;
import org.spearce.jgit.revplot.AbstractPlotRenderer;
import org.spearce.jgit.revplot.PlotCommit;

class SWTPlotRenderer extends AbstractPlotRenderer<SWTLane, Color> {
	private final Color sys_blue;

	private final Color sys_black;

	private final Color sys_gray;

	private Color sys_darkblue;

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
		g.drawString(msg, cellX + x, cellY + texty);
	}

	protected Color laneColor(final SWTLane myLane) {
		return myLane != null ? myLane.color : sys_black;
	}
}
