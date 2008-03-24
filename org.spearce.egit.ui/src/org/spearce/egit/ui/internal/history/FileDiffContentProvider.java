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
package org.spearce.egit.ui.internal.history;

import java.io.IOException;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.spearce.egit.ui.Activator;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.treewalk.TreeWalk;

class FileDiffContentProvider implements IStructuredContentProvider {
	private TreeWalk walk;

	private RevCommit commit;

	private FileDiff[] diff;

	public void inputChanged(final Viewer newViewer, final Object oldInput,
			final Object newInput) {
		walk = ((CommitFileDiffViewer) newViewer).getTreeWalk();
		commit = (RevCommit) newInput;
		diff = null;
	}

	public Object[] getElements(final Object inputElement) {
		if (diff == null && walk != null && commit != null) {
			try {
				diff = FileDiff.compute(walk, commit);
			} catch (IOException err) {
				Activator.error("Can't get file difference of "
						+ commit.getId() + ".", err);
			}
		}
		return diff;
	}

	public void dispose() {
		// Nothing.
	}
}
