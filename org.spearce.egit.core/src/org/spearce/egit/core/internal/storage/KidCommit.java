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
package org.spearce.egit.core.internal.storage;

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.revwalk.RevCommit;

class KidCommit extends RevCommit {
	static final KidCommit[] NO_CHILDREN = {};

	KidCommit[] children = NO_CHILDREN;

	KidCommit(final AnyObjectId id) {
		super(id);
	}

	void addChild(final KidCommit c) {
		final int cnt = children.length;
		if (cnt == 0)
			children = new KidCommit[] { c };
		else if (cnt == 1)
			children = new KidCommit[] { children[0], c };
		else {
			final KidCommit[] n = new KidCommit[cnt + 1];
			System.arraycopy(children, 0, n, 0, cnt);
			n[cnt] = c;
			children = n;
		}
	}

	@Override
	public void reset() {
		children = NO_CHILDREN;
		super.reset();
	}
}
