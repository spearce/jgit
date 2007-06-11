/*
 *  Copyright (C) 2006  Robin Rosenberg <robin.rosenberg@dewire.com>
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
package org.spearce.egit.core.op;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.spearce.egit.core.project.CheckpointJob;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;

/**
 * Performs a checkpoint (Update of the Git index).
 * <p>
 * Each project has a series of one or more <code>RepositoryMapping</code>s
 * associated with it, and each of these has a Git index. That tree defines the
 * current state of the Eclipse workspace, and is used to keep track of what the
 * user might intend on committing next. This operation updates the tree within
 * the Git object database to reflect what EGit has stored in memory.
 * </p>
 */
public class CheckpointOperation implements IWorkspaceRunnable {
	private final Collection<IResource> rsrcList;

	/**
	 * Create a new checkpoint operation for execution. A checkpoint updates
	 * all resources managed by a particular git repository. 
	 * 
	 * A git repo (and it's index) could manage a number of projects and even
	 * resources not visible in eclipse.
	 * 
	 * @param rsrcs
	 *            a collection of {@link IResource}s whose git indexes
	 *            should be updates.
	 */
	public CheckpointOperation(final Collection<IResource> rsrcs) {
		rsrcList = rsrcs;
	}

	public void run(IProgressMonitor m) throws CoreException {
		final Map<RepositoryMapping, RepositoryMapping> repositories = new IdentityHashMap<RepositoryMapping, RepositoryMapping>();
		for (IResource i : rsrcList) {
			final IProject project=i.getProject();
			final GitProjectData projectData = GitProjectData.get(project);
			RepositoryMapping rm = projectData.getRepositoryMapping(project);
			repositories.put(rm,rm);
		}
		for (RepositoryMapping i : repositories.keySet()) {
			new CheckpointJob(i).schedule();
		}
	}
}
