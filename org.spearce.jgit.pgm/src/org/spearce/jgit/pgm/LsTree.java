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

		walk.reset(); // drop the first empty tree, which we do not need here
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
