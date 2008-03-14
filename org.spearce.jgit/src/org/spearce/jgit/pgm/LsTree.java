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

import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.treewalk.TreeWalk;

class LsTree extends TextBuiltin {
	@Override
	void execute(final String[] args) throws Exception {
		final TreeWalk walk = new TreeWalk(db);
		final String name;
		if (args.length == 0)
			name = "HEAD^{tree}";
		else
			name = args[0];
		walk.addTree(resolve(name));
		walk.setRecursive(true);

		while (walk.next()) {
			final FileMode mode = FileMode.fromBits(walk.getRawMode(0));
			if (mode == FileMode.TREE)
				out.print('0');
			out.print(mode);
			out.print(mode == FileMode.TREE ? " tree" : " blob");

			out.print(' ');
			out.print(walk.getObjectId(0));

			out.print('\t');
			out.print(walk.getPathString());
			out.println();
		}
	}
}
