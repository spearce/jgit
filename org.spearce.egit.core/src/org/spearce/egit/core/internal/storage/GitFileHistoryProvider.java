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

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistoryProvider;

/**
 * A {@link FileHistoryProvider} for Git. This class has methods for retrieving
 * specific versions of a tracked resource.
 */
public class GitFileHistoryProvider extends FileHistoryProvider {
	public IFileHistory getFileHistoryFor(IResource resource, int flags,
			IProgressMonitor monitor) {
		return new GitFileHistory(resource, flags, monitor);
	}

	public IFileRevision getWorkspaceFileRevision(IResource resource) {
		return new WorkspaceFileRevision(resource);
	}

	public IFileHistory getFileHistoryFor(IFileStore store, int flags,
			IProgressMonitor monitor) {
		// TODO: implement getFileHistoryFor(IFileStore ...)
		return null;
	}
}
