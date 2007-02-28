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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.spearce.egit.core.project.GitProjectData;

/**
 * Performs a checkpoint (flush of the cache-tree to the Git database).
 * <p>
 * Each project has a series of one or more <code>RepositoryMapping</code>s
 * associated with it, and each of these has one Git tree stored within the Git
 * object database. That tree defines the current state of the Eclipse
 * workspace, and is used to keep track of what the user might intend on
 * committing next. This operation updates the tree within the Git object
 * database to reflect what EGit has stored in memory.
 * </p>
 * <p>
 * This operation currently only performs a checkpoint if necessary. That is, a
 * tree will only be written if the in memory structures indicate it is dirty.
 * If the dirty flags are incorrect, this operation will not do the right thing.
 * </p>
 */
public class CheckpointOperation implements IWorkspaceRunnable {
	private final Collection rsrcList;

	/**
	 * Create a new checkpoint operation for execution.
	 * 
	 * @param rsrcs
	 *            a collection of {@link IResource}s whose projects should have
	 *            their cache trees checkpointed. Since a project is an
	 *            IResource, this may just be a collection of projects.
	 *            Duplicate projects will be automatically filtered out.
	 */
	public CheckpointOperation(final Collection rsrcs) {
		rsrcList = rsrcs;
	}

	public void run(IProgressMonitor m) throws CoreException {
		final Set projects = new HashSet();
		for (final Iterator i = rsrcList.iterator(); i.hasNext();)
			projects.add(((IResource) i.next()).getProject());
		for (final Iterator i = projects.iterator(); i.hasNext();) {
			final IProject project = (IProject) i.next();
			final GitProjectData projectData = GitProjectData.get(project);
			if (projectData != null)
				projectData.fullUpdate();
		}
	}
}
