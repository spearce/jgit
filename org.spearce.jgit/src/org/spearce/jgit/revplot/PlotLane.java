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
 * A line space within the graph.
 * <p>
 * Commits are strung onto a lane. For many UIs a lane represents a column.
 */
public class PlotLane {
	PlotCommit parent;

	int position;

	/**
	 * Logical location of this lane within the graphing plane.
	 * 
	 * @return location of this lane, 0 through the maximum number of lanes.
	 */
	public int getPosition() {
		return position;
	}

	public int hashCode() {
		return position;
	}

	public boolean equals(final Object o) {
		return o == this;
	}
}
