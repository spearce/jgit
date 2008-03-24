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

import org.eclipse.swt.widgets.Widget;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.revplot.PlotCommit;

class SWTCommit extends PlotCommit<SWTCommitList.SWTLane> {
	Widget widget;

	SWTCommit(final AnyObjectId id) {
		super(id);
	}

	@Override
	public void reset() {
		widget = null;
		super.reset();
	}
}
