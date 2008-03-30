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
package org.spearce.jgit.treewalk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.util.FS;

/**
 * Working directory iterator for standard Java IO.
 * <p>
 * This iterator uses the standard <code>java.io</code> package to read the
 * specified working directory as part of a {@link TreeWalk}.
 */
public class FileTreeIterator extends WorkingTreeIterator {
	private final File directory;

	/**
	 * Create a new iterator to traverse the given directory and its children.
	 * 
	 * @param root
	 *            the starting directory. This directory should correspond to
	 *            the root of the repository.
	 */
	public FileTreeIterator(final File root) {
		directory = root;
	}

	/**
	 * Create a new iterator to traverse a subdirectory.
	 * 
	 * @param p
	 *            the parent iterator we were created from.
	 * @param root
	 *            the subdirectory. This should be a directory contained within
	 *            the parent directory.
	 */
	protected FileTreeIterator(final FileTreeIterator p, final File root) {
		super(p);
		directory = root;
	}

	@Override
	public AbstractTreeIterator createSubtreeIterator(final Repository repo)
			throws IncorrectObjectTypeException, IOException {
		return new FileTreeIterator(this, ((FileEntry) current()).file);
	}

	@Override
	protected Entry[] getEntries() {
		final File[] all = directory.listFiles();
		if (all == null)
			return EOF;
		final Entry[] r = new Entry[all.length];
		for (int i = 0; i < r.length; i++)
			r[i] = new FileEntry(all[i]);
		return r;
	}

	static class FileEntry extends Entry {
		final File file;

		private final FileMode mode;

		private long length = -1;

		FileEntry(final File f) {
			file = f;

			if (f.isDirectory()) {
				if (new File(f, ".git").isDirectory())
					mode = FileMode.GITLINK;
				else
					mode = FileMode.TREE;
			} else if (FS.INSTANCE.canExecute(file))
				mode = FileMode.EXECUTABLE_FILE;
			else
				mode = FileMode.REGULAR_FILE;
		}

		@Override
		public FileMode getMode() {
			return mode;
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public long getLength() {
			if (length < 0)
				length = file.length();
			return length;
		}

		@Override
		public InputStream openInputStream() throws IOException {
			return new FileInputStream(file);
		}
	}
}
