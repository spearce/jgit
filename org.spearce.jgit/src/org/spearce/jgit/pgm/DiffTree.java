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

import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.treewalk.TreeWalk;
import org.spearce.jgit.treewalk.filter.AndTreeFilter;
import org.spearce.jgit.treewalk.filter.OrTreeFilter;
import org.spearce.jgit.treewalk.filter.PathFilter;
import org.spearce.jgit.treewalk.filter.TreeFilter;

class DiffTree extends TextBuiltin {
	@Override
	void execute(String[] args) throws Exception {
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
