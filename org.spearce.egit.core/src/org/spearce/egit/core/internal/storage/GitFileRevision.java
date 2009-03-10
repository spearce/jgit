/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.internal.storage;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommit;

/**
 * A Git related {@link IFileRevision}. It references a version and a resource,
 * i.e. the version we think corresponds to the resource in specific version.
 */
public abstract class GitFileRevision extends FileRevision {
	/** Content identifier for the working copy. */
	public static final String WORKSPACE = "Workspace";

	/** Content identifier for the content staged in the index. */
	public static final String INDEX = "Index";

	/**
	 * Obtain a file revision for a specific blob of an existing commit.
	 * 
	 * @param db
	 *            the repository this commit was loaded out of, and that this
	 *            file's blob should also be reachable through.
	 * @param commit
	 *            the commit the blob was identified to be within.
	 * @param path
	 *            path within the commit's tree of the file.
	 * @param blobId
	 *            unique name of the content.
	 * @return revision implementation for this file in the given commit.
	 */
	public static GitFileRevision inCommit(final Repository db,
			final RevCommit commit, final String path, final ObjectId blobId) {
		return new CommitFileRevision(db, commit, path, blobId);
	}

	/**
	 * @param db
	 *            the repository which contains the index to use.
	 * @param path
	 *            path of the resource in the index
	 * @return revision implementation for the given path in the index
	 */
	public static GitFileRevision inIndex(final Repository db, final String path) {
		return new IndexFileRevision(db, path);
	}

	private final String path;

	GitFileRevision(final String fileName) {
		path = fileName;
	}

	public String getName() {
		final int last = path.lastIndexOf('/');
		return last >= 0 ? path.substring(last + 1) : path;
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(final IProgressMonitor monitor)
			throws CoreException {
		return this;
	}

	public URI getURI() {
		try {
			return new URI(null, null, path, null);
		} catch (URISyntaxException e) {
			return null;
		}
	}
}
