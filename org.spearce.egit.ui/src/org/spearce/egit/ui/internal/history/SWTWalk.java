/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revplot.PlotWalk;
import org.spearce.jgit.revwalk.RevCommit;

class SWTWalk extends PlotWalk {
	SWTWalk(final Repository repo) {
		super(repo);
	}

	@Override
	protected RevCommit createCommit(final AnyObjectId id) {
		return new SWTCommit(id, getTags(id));
	}
}
