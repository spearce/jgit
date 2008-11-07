/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import org.eclipse.swt.widgets.Widget;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.revplot.PlotCommit;
import org.spearce.jgit.lib.Ref;

class SWTCommit extends PlotCommit<SWTCommitList.SWTLane> {
	Widget widget;

	SWTCommit(final AnyObjectId id, final Ref[] tags) {
		super(id, tags);
	}

	@Override
	public void reset() {
		widget = null;
		super.reset();
	}
}
