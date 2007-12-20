/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Abstract TreeVisitor for visiting all files known by a Tree.
 */
public abstract class TreeVisitorWithCurrentDirectory implements TreeVisitor {
	private final ArrayList stack;

	private File currentDirectory;

	protected TreeVisitorWithCurrentDirectory(final File rootDirectory) {
		stack = new ArrayList(16);
		currentDirectory = rootDirectory;
	}

	protected File getCurrentDirectory() {
		return currentDirectory;
	}

	public void startVisitTree(final Tree t) throws IOException {
		stack.add(currentDirectory);
		if (!t.isRoot()) {
			currentDirectory = new File(currentDirectory, t.getName());
		}
	}

	public void endVisitTree(final Tree t) throws IOException {
		currentDirectory = (File) stack.remove(stack.size() - 1);
	}
}
