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
package org.spearce.egit.core.internal.mapping;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistoryProvider;
import org.spearce.egit.core.GitWorkspaceFileRevision;

/**
 * A {@link FileHistoryProvider} for Git. This class has methods for
 * retrieving specific versions of a tracked resource.
 */
public class GitFileHistoryProvider extends FileHistoryProvider implements
		IFileHistoryProvider {

	public IFileHistory getFileHistoryFor(IResource resource, int flags,
			IProgressMonitor monitor) {
		return new GitFileHistory(resource, flags, monitor, false);
	}

	public IFileRevision getWorkspaceFileRevision(IResource resource) {
		return new GitWorkspaceFileRevision(resource, -1);
	}

	public IFileHistory getFileHistoryFor(IFileStore store, int flags,
			IProgressMonitor monitor) {
		// TODO: implement flags and monitor
		return null;
	}

	/**
	 * Get the history for a given resource as viewed using the specified
	 * filtering flags.
	 *
	 * @param resource
	 * @param flags
	 *            See {@link IFileHistoryProvider}
	 * @param monitor
	 *            progress monitor
	 * @param returnAll
	 *            true if all revisions are returned and interesting revisions marked.
	 *
	 * @return a {@link GitFileHistory} object.
	 *
	 */
	public GitFileHistory getHistoryFor(IResource resource, int flags, IProgressMonitor monitor, boolean returnAll) {
		return new GitFileHistory(resource, flags, monitor, returnAll); // TODO: implement flags
	}

}
