/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core;

import java.io.File;
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
				if (index.remove(map.getWorkDir(), file.getLocation().toFile()))
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
				// Source is not in a Git controlled project, fine
				return FINISH_FOR_ME;
			}
			final GitIndex index1 = map1.getRepository().getIndex();
			final RepositoryMapping map2 = RepositoryMapping.getMapping(destination);
			final File sourceFile = source.getLocation().toFile();
			if (map2 == null) {
				if (index1.getEntry(Repository.stripWorkDir(map1.getWorkDir(), sourceFile)) == null) {
					// if the source resource is not tracked by Git that is ok too
					return FINISH_FOR_ME;
				}
				tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
						0, "Destination not in a git versioned project", null));
				return NOT_ALLOWED;
			}
			GitIndex index2 = map2.getRepository().getIndex();
			tree.standardMoveFile(source, destination, updateFlags, monitor);
			if (index1.remove(map1.getWorkDir(), sourceFile)) {
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