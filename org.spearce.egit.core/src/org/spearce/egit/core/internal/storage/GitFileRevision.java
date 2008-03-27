/*
 *  Copyright (C) 2006  Robin Rosenberg
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
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
