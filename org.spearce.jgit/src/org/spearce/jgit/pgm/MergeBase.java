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
package org.spearce.jgit.pgm;

import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.revwalk.filter.RevFilter;

class MergeBase extends TextBuiltin {
	@Override
	void execute(final String[] args) throws Exception {
		final RevWalk walk = new RevWalk(db);
		int max = 1;
		for (final String a : args) {
			if ("--".equals(a))
				break;
			else if ("--all".equals(a))
				max = Integer.MAX_VALUE;
			else
				walk.markStart(walk.parseCommit(resolve(a)));
		}

		walk.setRevFilter(RevFilter.MERGE_BASE);
		while (max-- > 0) {
			final RevCommit b = walk.next();
			if (b == null)
				break;
			out.println(b.getId());
		}
	}
}
