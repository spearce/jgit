/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.internal.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.ITag;
import org.spearce.egit.core.GitTag;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.treewalk.TreeWalk;

/**
 * An {@link IFileRevision} for a version of a specified resource in the
 * specified commit (revision).
 */
class CommitFileRevision extends GitFileRevision {
	private final Repository db;

	private final RevCommit commit;

	private final PersonIdent author;

	private final String path;

	private ObjectId blobId;

	CommitFileRevision(final Repository repo, final RevCommit rc,
			final String fileName) {
		this(repo, rc, fileName, null);
	}

	CommitFileRevision(final Repository repo, final RevCommit rc,
			final String fileName, final ObjectId blob) {
		super(fileName);
		db = repo;
		commit = rc;
		author = rc.getAuthorIdent();
		path = fileName;
		blobId = blob;
	}

	String getGitPath() {
		return path;
	}

	public IStorage getStorage(final IProgressMonitor monitor)
			throws CoreException {
		if (blobId == null)
			blobId = locateBlobObjectId();
		return new BlobStorage(db, path, blobId);
	}

	public long getTimestamp() {
		return author != null ? author.getWhen().getTime() : 0;
	}

	public String getContentIdentifier() {
		return commit.getId().name();
	}

	public String getAuthor() {
		return author != null ? author.getName() : null;
	}

	public String getComment() {
		return commit.getShortMessage();
	}

	public String toString() {
		return commit.getId() + ":" + path;
	}

	public ITag[] getTags() {
		final Collection<GitTag> ret = new ArrayList<GitTag>();
		for (final Map.Entry<String, Ref> tag : db.getTags().entrySet()) {
			final ObjectId ref = tag.getValue().getPeeledObjectId();
			if (ref == null)
				continue;
			if (!AnyObjectId.equals(ref, commit))
				continue;
			ret.add(new GitTag(tag.getKey()));
		}
		return ret.toArray(new ITag[ret.size()]);
	}

	/**
	 * Get the commit that introduced this file revision.
	 * 
	 * @return the commit we most recently noticed this file in.
	 */
	public RevCommit getRevCommit() {
		return commit;
	}

	private ObjectId locateBlobObjectId() throws CoreException {
		try {
			final TreeWalk w = TreeWalk.forPath(db, path, commit.getTree());
			if (w == null)
				throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL,
						Path.fromPortableString(path), "Path not in "
								+ commit.getId() + ".", null);
			return w.getObjectId(0);
		} catch (IOException e) {
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, Path
					.fromPortableString(path), "IO error looking up path in "
					+ commit.getId() + ".", e);
		}
	}
}
