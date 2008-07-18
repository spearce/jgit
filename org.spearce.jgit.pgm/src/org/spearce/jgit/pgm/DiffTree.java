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
import java.util.List;

import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.treewalk.TreeWalk;
import org.spearce.jgit.treewalk.filter.AndTreeFilter;
import org.spearce.jgit.treewalk.filter.OrTreeFilter;
import org.spearce.jgit.treewalk.filter.PathFilter;
import org.spearce.jgit.treewalk.filter.TreeFilter;

class DiffTree extends TextBuiltin {
	@Override
	public void execute(String[] args) throws Exception {
		final TreeWalk walk = new TreeWalk(db);
		final List<String> argList = new ArrayList<String>();
		List<TreeFilter> pathLimiter = null;
		for (final String a : args) {
			if (pathLimiter != null)
				pathLimiter.add(PathFilter.create(a));
			else if ("--".equals(a))
				pathLimiter = new ArrayList<TreeFilter>();
			else if ("-r".equals(a))
				walk.setRecursive(true);
			else
				argList.add(a);
		}

		final TreeFilter pathFilter;
		if (pathLimiter == null || pathLimiter.isEmpty())
			pathFilter = TreeFilter.ALL;
		else if (pathLimiter.size() == 1)
			pathFilter = pathLimiter.get(0);
		else
			pathFilter = OrTreeFilter.create(pathLimiter);
		walk.setFilter(AndTreeFilter.create(TreeFilter.ANY_DIFF, pathFilter));

		if (argList.size() == 0)
			argList.add("HEAD");
		if (argList.size() == 1) {
			final String a = argList.get(0);
			argList.clear();
			argList.add(a + "^^{tree}");
			argList.add(a + "^{tree}");
		}
		for (final String a : argList)
			walk.addTree(resolve(a));

		final int nTree = walk.getTreeCount();
		while (walk.next()) {
			for (int i = 1; i < nTree; i++)
				out.print(':');
			for (int i = 0; i < nTree; i++) {
				final FileMode m = walk.getFileMode(i);
				final String s = m.toString();
				for (int pad = 6 - s.length(); pad > 0; pad--)
					out.print('0');
				out.print(s);
				out.print(' ');
			}

			for (int i = 0; i < nTree; i++) {
				out.print(walk.getObjectId(i));
				out.print(' ');
			}

			char chg = 'M';
			if (nTree == 2) {
				final int m0 = walk.getRawMode(0);
				final int m1 = walk.getRawMode(1);
				if (m0 == 0 && m1 != 0)
					chg = 'A';
				else if (m0 != 0 && m1 == 0)
					chg = 'D';
				else if (m0 != m1 && walk.idEqual(0, 1))
					chg = 'T';
			}
			out.print(chg);

			out.print('\t');
			out.print(walk.getPathString());
			out.println();
		}
	}
}
