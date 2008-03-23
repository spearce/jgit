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

import java.util.ArrayList;
import java.util.List;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevSort;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.revwalk.filter.AndRevFilter;
import org.spearce.jgit.revwalk.filter.AuthorRevFilter;
import org.spearce.jgit.revwalk.filter.CommitterRevFilter;
import org.spearce.jgit.revwalk.filter.MessageRevFilter;
import org.spearce.jgit.revwalk.filter.RevFilter;
import org.spearce.jgit.treewalk.filter.AndTreeFilter;
import org.spearce.jgit.treewalk.filter.PathFilter;
import org.spearce.jgit.treewalk.filter.PathFilterGroup;
import org.spearce.jgit.treewalk.filter.TreeFilter;

abstract class RevWalkTextBuiltin extends TextBuiltin {
	RevWalk walk;

	boolean parents = false;

	boolean count = false;

	char[] outbuffer = new char[Constants.OBJECT_ID_LENGTH * 2];

	@Override
	final void execute(String[] args) throws Exception {
		walk = new RevWalk(db);

		final List<String> argList = new ArrayList<String>();
		final List<RevFilter> revLimiter = new ArrayList<RevFilter>();
		List<PathFilter> pathLimiter = null;
		for (final String a : args) {
			if (pathLimiter != null)
				pathLimiter.add(PathFilter.create(a));
			else if ("--".equals(a))
				pathLimiter = new ArrayList<PathFilter>();
			else if (a.startsWith("--author="))
				revLimiter.add(AuthorRevFilter.create(a.substring("--author="
						.length())));
			else if (a.startsWith("--committer="))
				revLimiter.add(CommitterRevFilter.create(a
						.substring("--committer=".length())));
			else if (a.startsWith("--grep="))
				revLimiter.add(MessageRevFilter.create(a.substring("--grep="
						.length())));
			else if ("--date-order".equals(a))
				walk.sort(RevSort.COMMIT_TIME_DESC, true);
			else if ("--topo-order".equals(a))
				walk.sort(RevSort.TOPO, true);
			else if ("--reverse".equals(a))
				walk.sort(RevSort.REVERSE, true);
			else if ("--parents".equals(a))
				parents = true;
			else if ("--total-count".equals(a))
				count = true;
			else
				argList.add(a);
		}

		if (pathLimiter != null && !pathLimiter.isEmpty())
			walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup
					.create(pathLimiter), TreeFilter.ANY_DIFF));

		if (revLimiter.size() == 1)
			walk.setRevFilter(revLimiter.get(0));
		else if (revLimiter.size() > 1)
			walk.setRevFilter(AndRevFilter.create(revLimiter));

		final long start = System.currentTimeMillis();
		boolean have_revision = false;
		boolean not = false;
		for (String a : argList) {
			if ("--not".equals(a)) {
				not = true;
				continue;
			}
			boolean menot = false;
			if (a.startsWith("^")) {
				a = a.substring(1);
				menot = true;
			}
			final RevCommit c = walk.parseCommit(resolve(a));
			if (not ^ menot)
				walk.markUninteresting(c);
			else {
				walk.markStart(c);
				have_revision = true;
			}
		}
		if (!have_revision)
			walk.markStart(walk.parseCommit(resolve("HEAD")));

		int n = 0;
		for (final RevCommit c : walk) {
			n++;
			show(c);
		}

		if (count) {
			final long end = System.currentTimeMillis();
			System.err.print(n);
			System.err.print(' ');
			System.err.print(end - start);
			System.err.print(" ms");
			System.err.println();
		}
	}

	protected abstract void show(final RevCommit c) throws Exception;
}
