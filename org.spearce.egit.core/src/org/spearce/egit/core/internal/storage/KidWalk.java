/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.internal.storage;

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevWalk;

class KidWalk extends RevWalk {
	KidWalk(final Repository repo) {
		super(repo);
	}

	@Override
	protected RevCommit createCommit(final AnyObjectId id) {
		return new KidCommit(id);
	}
}
