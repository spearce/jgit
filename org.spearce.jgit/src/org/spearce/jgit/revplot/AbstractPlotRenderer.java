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
package org.spearce.jgit.revplot;

/**
 * Basic commit graph renderer for graphical user interfaces.
 * <p>
 * Lanes are drawn as columns left-to-right in the graph, and the commit short
 * message is drawn to the right of the lane lines for this cell. It is assumed
 * that the commits are being drawn as rows of some sort of table.
 * <p>
 * Client applications can subclass this implementation to provide the necessary
 * drawing primitives required to display a commit graph. Most of the graph
 * layout is handled by this class, allowing applications to implement only a
 * handful of primitive stubs.
 * <p>
 * This class is suitable for us within an AWT TableCellRenderer or within a SWT
 * PaintListener registered on a Table instance. It is meant to rubber stamp the
 * graphics necessary for one row of a plotted commit list.
 * <p>
 * Subclasses should call {@link #paintCommit(PlotCommit, int)} after they have
 * otherwise configured their instance to draw one commit into the current
 * location.
 * <p>
 * All drawing methods assume the coordinate space for the current commit's cell
 * starts at (upper left corner is) 0,0. If this is not true (like say in SWT)
 * the implementation must perform the cell offset computations within the
 * various draw methods.
 * 
 * @param <TLane>
 *            type of lane being used by the application.
 * @param <TColor>
 *            type of color object used by the graphics library.
 */
public abstract class AbstractPlotRenderer<TLane extends PlotLane, TColor> {
	private static final int LANE_WIDTH = 15;

	private static final int LEFT_PAD = 2;

	/**
	 * Paint one commit using the underlying graphics library.
	 * 
	 * @param commit
	 *            the commit to render in this cell. Must not be null.
	 * @param h
	 *            total height (in pixels) of this cell.
	 */
	protected void paintCommit(final PlotCommit<TLane> commit, final int h) {
		final int dotSize = Math.max(0, Math.min(8, h - 4));
		final TLane myLane = commit.getLane();
		final int myLaneX = laneC(myLane);

		int maxCenter = 0;
		for (final TLane passingLane : (TLane[]) commit.passingLanes) {
			final int cx = laneC(passingLane);
			final TColor c = laneColor(passingLane);
			drawLine(c, cx, 0, cx, h);
			maxCenter = Math.max(maxCenter, cx);
		}

		final int nParent = commit.getParentCount();
		for (int i = 0; i < nParent; i++) {
			final PlotCommit<TLane> p;
			final TLane pLane;
			final TColor pColor;
			final int cx;

			p = (PlotCommit<TLane>) commit.getParent(i);
			pLane = p.getLane();
			pColor = laneColor(pLane);
			cx = laneC(pLane);

			if (Math.abs(myLaneX - cx) > LANE_WIDTH) {
				if (myLaneX < cx) {
					final int ix = cx - LANE_WIDTH / 2;
					drawLine(pColor, myLaneX, h / 2, ix, h / 2);
					drawLine(pColor, ix, h / 2, cx, h);
				} else {
					final int ix = cx + LANE_WIDTH / 2;
					drawLine(pColor, myLaneX, h / 2, ix, h / 2);
					drawLine(pColor, ix, h / 2, cx, h);
				}
			} else {
				drawLine(pColor, myLaneX, h / 2, cx, h);
			}
			maxCenter = Math.max(maxCenter, cx);
		}

		final int dotX = myLaneX - dotSize / 2;
		final int dotY = (h - dotSize) / 2;

		if (commit.getChildCount() > 0)
			drawLine(laneColor(myLane), myLaneX, 0, myLaneX, dotY);
		drawCommitDot(dotX, dotY, dotSize, dotSize);

		final String msg = commit.getShortMessage();
		final int textx = Math.max(maxCenter + LANE_WIDTH / 2, dotX + dotSize) + 8;
		drawText(msg, textx, h / 2);
	}

	/**
	 * Obtain the color reference used to paint this lane.
	 * <p>
	 * Colors returned by this method will be passed to the other drawing
	 * primitives, so the color returned should be application specific.
	 * <p>
	 * If a null lane is supplied the return value must still be acceptable to a
	 * drawing method. Usually this means the implementation should return a
	 * default color.
	 * 
	 * @param myLane
	 *            the current lane. May be null.
	 * @return graphics specific color reference. Must be a valid color.
	 */
	protected abstract TColor laneColor(TLane myLane);

	/**
	 * Draw a single line within this cell.
	 * 
	 * @param color
	 *            the color to use while drawing the line.
	 * @param x1
	 *            starting X coordinate, 0 based.
	 * @param y1
	 *            starting Y coordinate, 0 based.
	 * @param x2
	 *            ending X coordinate, 0 based.
	 * @param y2
	 *            ending Y coordinate, 0 based.
	 */
	protected abstract void drawLine(TColor color, int x1, int y1, int x2,
			int y2);

	/**
	 * Draw a single commit dot.
	 * <p>
	 * Usually the commit dot is a filled oval in blue, then a drawn oval in
	 * black, using the same coordinates for both operations.
	 * 
	 * @param x
	 *            upper left of the oval's bounding box.
	 * @param y
	 *            upper left of the oval's bounding box.
	 * @param w
	 *            width of the oval's bounding box.
	 * @param h
	 *            height of the oval's bounding box.
	 */
	protected abstract void drawCommitDot(int x, int y, int w, int h);

	/**
	 * Draw a single line of text.
	 * <p>
	 * The font and colors used to render the text are left up to the
	 * implementation.
	 * 
	 * @param msg
	 *            the text to draw. Does not contain LFs.
	 * @param x
	 *            first pixel from the left that the text can be drawn at.
	 *            Character data must not appear before this position.
	 * @param y
	 *            pixel coordinate of the centerline of the text.
	 *            Implementations must adjust this coordinate to account for the
	 *            way their implementation handles font rendering.
	 */
	protected abstract void drawText(String msg, int x, int y);

	private int laneX(final PlotLane myLane) {
		final int p = myLane != null ? myLane.getPosition() : 0;
		return LEFT_PAD + LANE_WIDTH * p;
	}

	private int laneC(final PlotLane myLane) {
		return laneX(myLane) + LANE_WIDTH / 2;
	}
}
