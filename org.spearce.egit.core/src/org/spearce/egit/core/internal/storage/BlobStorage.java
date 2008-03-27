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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.Repository;

/** Accesses a blob from Git. */
class BlobStorage implements IStorage {
	private final Repository db;

	private final String path;

	private final ObjectId blobId;

	BlobStorage(final Repository repository, final String fileName,
			final ObjectId blob) {
		db = repository;
		path = fileName;
		blobId = blob;
	}

	public InputStream getContents() throws CoreException {
		try {
			return open();
		} catch (IOException e) {
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL,
					getFullPath(), "IO error reading Git blob " + blobId + ".",
					e);
		}
	}

	private InputStream open() throws IOException, ResourceException,
			IncorrectObjectTypeException {
		final ObjectLoader reader = db.openBlob(blobId);
		if (reader == null)
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL,
					getFullPath(), "Git blob " + blobId + " not found.", null);
		final byte[] data = reader.getBytes();
		if (reader.getType() != Constants.OBJ_BLOB)
			throw new IncorrectObjectTypeException(blobId, Constants.TYPE_BLOB);
		return new ByteArrayInputStream(data);
	}

	public IPath getFullPath() {
		return Path.fromPortableString(path);
	}

	public String getName() {
		final int last = path.lastIndexOf('/');
		return last >= 0 ? path.substring(last + 1) : path;
	}

	public boolean isReadOnly() {
		return true;
	}

	public Object getAdapter(final Class adapter) {
		return null;
	}
}
