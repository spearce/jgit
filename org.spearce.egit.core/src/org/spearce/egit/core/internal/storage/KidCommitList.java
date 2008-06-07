/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.internal.storage;

import org.spearce.jgit.revwalk.RevCommitList;

class KidCommitList extends RevCommitList<KidCommit> {
	@Override
	protected void enter(final int index, final KidCommit e) {
		final int nParents = e.getParentCount();
		for (int i = 0; i < nParents; i++)
			((KidCommit) e.getParent(i)).addChild(e);
	}
}
