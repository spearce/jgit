/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.pgm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.revwalk.ObjectWalk;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevObject;
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

	boolean objects = false;

	boolean parents = false;

	boolean count = false;

	char[] outbuffer = new char[Constants.OBJECT_ID_LENGTH * 2];

	@Override
	final void execute(String[] args) throws Exception {
		final EnumSet<RevSort> sorting = EnumSet.noneOf(RevSort.class);
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
			else if ("--objects".equals(a))
				objects = true;
			else if ("--date-order".equals(a))
				sorting.add(RevSort.COMMIT_TIME_DESC);
			else if ("--topo-order".equals(a))
				sorting.add(RevSort.TOPO);
			else if ("--reverse".equals(a))
				sorting.add(RevSort.REVERSE);
			else if ("--boundary".equals(a))
				sorting.add(RevSort.BOUNDARY);
			else if ("--parents".equals(a))
				parents = true;
			else if ("--total-count".equals(a))
				count = true;
			else
				argList.add(a);
		}

		walk = createWalk();
		for (final RevSort s : sorting)
			walk.sort(s, true);

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

		int n = walkLoop();

		if (count) {
			final long end = System.currentTimeMillis();
			System.err.print(n);
			System.err.print(' ');
			System.err.print(end - start);
			System.err.print(" ms");
			System.err.println();
		}
	}

	protected RevWalk createWalk() {
		if (objects)
			return new ObjectWalk(db);
		return new RevWalk(db);
	}

	protected int walkLoop() throws Exception {
		int n = 0;
		for (final RevCommit c : walk) {
			n++;
			show(c);
		}
		if (walk instanceof ObjectWalk) {
			final ObjectWalk ow = (ObjectWalk) walk;
			for (;;) {
				final RevObject obj = ow.nextObject();
				if (obj == null)
					break;
				show(ow, obj);
			}
		}
		return n;
	}

	protected abstract void show(final RevCommit c) throws Exception;

	protected void show(final ObjectWalk objectWalk,
			final RevObject currentObject) throws Exception {
		// Do nothing by default. Most applications cannot show an object.
	}
}
