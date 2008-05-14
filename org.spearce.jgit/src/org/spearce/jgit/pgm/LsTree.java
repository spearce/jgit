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

import java.io.File;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.treewalk.FileTreeIterator;
import org.spearce.jgit.treewalk.TreeWalk;

class LsTree extends TextBuiltin {
	@Override
	void execute(final String[] args) throws Exception {
		final TreeWalk walk = new TreeWalk(db);
		int argi = 0;
		for (; argi < args.length; argi++) {
			final String a = args[argi];
			if ("--".equals(a)) {
				argi++;
				break;
			} else if ("-r".equals(a))
				walk.setRecursive(true);
			else
				break;
		}

		if (argi == args.length)
			throw die("usage: [-r] treename");
		else if (argi + 1 < args.length)
			throw die("too many arguments");

		final String n = args[argi];
		if (is_WorkDir(n))
			walk.addTree(new FileTreeIterator(new File(n)));
		else
			walk.addTree(resolve(n));

		while (walk.next()) {
			final FileMode mode = walk.getFileMode(0);
			if (mode == FileMode.TREE)
				out.print('0');
			out.print(mode);
			out.print(' ');
			out.print(Constants.typeString(mode.getObjectType()));

			out.print(' ');
			out.print(walk.getObjectId(0));

			out.print('\t');
			out.print(walk.getPathString());
			out.println();
		}
	}

	private boolean is_WorkDir(final String name) {
		return new File(name).isDirectory();
	}
}
