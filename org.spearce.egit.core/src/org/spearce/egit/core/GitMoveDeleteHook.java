/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.Repository;

class GitMoveDeleteHook implements IMoveDeleteHook {
	private static final boolean NOT_ALLOWED = true;
	private static final boolean I_AM_DONE = true;

	private static final boolean FINISH_FOR_ME = false;

	private final GitProjectData data;

	GitMoveDeleteHook(final GitProjectData d) {
		Assert.isNotNull(d);
		data = d;
	}

	public boolean deleteFile(final IResourceTree tree, final IFile file,
			final int updateFlags, final IProgressMonitor monitor) {
		try {
			RepositoryMapping map = RepositoryMapping.getMapping(file);
			if (map != null) {
				Repository repository = map.getRepository();
				GitIndex index = repository.getIndex();
				index.remove(map.getWorkDir(), file.getLocation().toFile());
				index.write();
			}
			return FINISH_FOR_ME;
		} catch (IOException e) {
			e.printStackTrace();
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
			return NOT_ALLOWED;
		}
	}

	public boolean deleteFolder(final IResourceTree tree, final IFolder folder,
			final int updateFlags, final IProgressMonitor monitor) {
		// Deleting a GIT repository which is in use is a pretty bad idea. To
		// delete disconnect the team provider first.
		//
		if (data.isProtected(folder)) {
			return cannotModifyRepository(tree);
		} else {
			return FINISH_FOR_ME;
		}
	}

	public boolean deleteProject(final IResourceTree tree,
			final IProject project, final int updateFlags,
			final IProgressMonitor monitor) {
		// TODO: Note that eclipse thinks folders are real, while
		// Git does not care.
		return FINISH_FOR_ME;
	}

	public boolean moveFile(final IResourceTree tree, final IFile source,
			final IFile destination, final int updateFlags,
			final IProgressMonitor monitor) {
		try {
			final RepositoryMapping map1 = RepositoryMapping.getMapping(source);
			if (map1 == null) {
				tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
						0, "Not in a git versioned project", null));
				return NOT_ALLOWED;
			}

			final GitIndex index1 = map1.getRepository().getIndex();

			final RepositoryMapping map2 = RepositoryMapping.getMapping(destination);
			if (map2 == null) {
				tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
						0, "Not in a git versioned project", null));
				return NOT_ALLOWED;
			}
			GitIndex index2 = map2.getRepository().getIndex();
			tree.standardMoveFile(source, destination, updateFlags, monitor);
			if (index1.remove(map1.getWorkDir(), source.getLocation().toFile())) {
				index2.add(map2.getWorkDir(), destination.getLocation().toFile());
				index1.write();
				if (index2 != index1)
					index2.write();
			}
			return I_AM_DONE;

		} catch (IOException e) {
			// Recover properly!
			e.printStackTrace();
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
			return NOT_ALLOWED;

		}
	}

	public boolean moveFolder(final IResourceTree tree, final IFolder source,
			final IFolder destination, final int updateFlags,
			final IProgressMonitor monitor) {
		// TODO: Implement this. Should be relatively easy, but consider that
		// Eclipse thinks folders are real thinsgs, while Git does not care.
		return NOT_ALLOWED;
	}

	public boolean moveProject(final IResourceTree tree, final IProject source,
			final IProjectDescription description, final int updateFlags,
			final IProgressMonitor monitor) {
		// Never allow moving a project as it can complicate updating our
		// project data with the new repository mappings. To move a project
		// disconnect the GIT team provider, move the project, then reconnect
		// the GIT team provider.
		// We should be able to do this without too much effort when the
		// projects belong to the same Git repository.
		//
		return NOT_ALLOWED;
	}

	private boolean cannotModifyRepository(final IResourceTree tree) {
		tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
				CoreText.MoveDeleteHook_cannotModifyFolder, null));
		return NOT_ALLOWED;
	}
}