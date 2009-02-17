/*******************************************************************************
 * Copyright (C) 2009, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/

package org.spearce.egit.core;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Path;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.treewalk.AbstractTreeIterator;
import org.spearce.jgit.treewalk.FileTreeIterator;

/**
 * Java IO file tree iterator that can adapt to a {@link ContainerTreeIterator}
 * <p>
 * The iterator automatically adapts to a {@link ContainerTreeIterator} when
 * recursing into directories that are accessible from the given workspace root.
 *
 * @see org.spearce.jgit.treewalk.FileTreeIterator
 * @see org.spearce.egit.core.ContainerTreeIterator
 */
public class AdaptableFileTreeIterator extends FileTreeIterator {

	IWorkspaceRoot root;

	/**
	 * Create a new iterator to traverse the given directory and its children
	 * <p>
	 * The iterator will automatically adapt to a {@link ContainerTreeIterator}
	 * when encountering directories what can be mapped into the given workspace
	 * root.
	 *
	 * @param path
	 *            the starting directory. This directory should correspond to
	 *            the repository root.
	 * @param workspaceRoot
	 *            the workspace root to check resource mapping against.
	 *
	 */
	public AdaptableFileTreeIterator(final File path,
			final IWorkspaceRoot workspaceRoot) {
		super(path);
		root = workspaceRoot;
	}

	/**
	 * Create a new iterator to traverse a subdirectory.
	 * <p>
	 * The iterator will automatically adapt to a {@link ContainerTreeIterator}
	 * when encountering directories what can be mapped into the given workspace
	 * root.
	 *
	 * @param path
	 *            the subdirectory. This should be a directory contained within
	 *            the parent directory.
	 * @param parent
	 *            the parent iterator we were created from.
	 * @param workspaceRoot
	 *            the workspace root to check resource mapping against.
	 */
	protected AdaptableFileTreeIterator(final AdaptableFileTreeIterator parent,
			File path, final IWorkspaceRoot workspaceRoot) {
		super(parent, path);
		root = workspaceRoot;
	}

	@Override
	public AbstractTreeIterator createSubtreeIterator(Repository repo)
			throws IncorrectObjectTypeException, IOException {
		final File currentFile = ((FileEntry) current()).getFile();
		final IContainer[] containers = root.findContainersForLocation(new Path(
				currentFile.getAbsolutePath()));
		if (containers.length > 0)
			return new ContainerTreeIterator(this, containers[0]);
		else
			return new AdaptableFileTreeIterator(this, currentFile, root);
	}
}
