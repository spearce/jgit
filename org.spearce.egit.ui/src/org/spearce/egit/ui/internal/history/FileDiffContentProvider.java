/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
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
