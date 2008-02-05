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
package org.spearce.egit.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.spearce.egit.core.internal.mapping.GitFileRevision;
import org.spearce.jgit.lib.ObjectId;

/**
 * An {@link IFileRevision} for the version in the Git index.
 */
public class GitIndexFileRevision extends GitFileRevision implements
		IFileRevision {

	/**
	 * Construct a GitIndexFileRevision matching a certain resource.
	 * @param resource The corresponding workspace resource
	 * @param count index into the full list of results
	 */
	public GitIndexFileRevision(IResource resource, int count) {
		super(resource, count);
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		return new GitStorage(ObjectId.zeroId(), getResource());
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
		return "Index";
	}
}
