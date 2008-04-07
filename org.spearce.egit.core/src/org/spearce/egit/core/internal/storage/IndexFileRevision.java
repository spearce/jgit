/*
 *  Copyright (C) 2007  Robin Rosenberg
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

import java.io.IOException;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.history.IFileRevision;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.GitIndex.Entry;

/** An {@link IFileRevision} for the version in the Git index. */
class IndexFileRevision extends GitFileRevision implements IFileRevision {
	private final Repository db;

	private final String path;

	private ObjectId blobId;

	IndexFileRevision(final Repository repo, final String fileName) {
		super(fileName);
		db = repo;
		path = fileName;
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		if (blobId == null)
			blobId = locateBlobObjectId();
		return new BlobStorage(db, path, blobId);
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(IProgressMonitor monitor)
			throws CoreException {
		return null;
	}

	public String getAuthor() {
		return "";
	}

	public long getTimestamp() {
		return -1;
	}

	public String getComment() {
		return null;
	}

	public String getContentIdentifier() {
		return INDEX;
	}

	private ObjectId locateBlobObjectId() throws CoreException {
		try {
			final GitIndex idx = db.getIndex();
			final Entry e = idx.getEntry(path);
			if (e == null)
				throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL,
						Path.fromPortableString(path),
						"Git index entry not found", null);
			return e.getObjectId();

		} catch (IOException e) {
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, Path
					.fromPortableString(path),
					"IO error looking up path in index.", e);
		}
	}
}
