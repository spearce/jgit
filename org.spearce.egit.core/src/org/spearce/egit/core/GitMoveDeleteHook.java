/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.dircache.DirCache;
import org.spearce.jgit.dircache.DirCacheBuilder;
import org.spearce.jgit.dircache.DirCacheEditor;
import org.spearce.jgit.dircache.DirCacheEntry;

class GitMoveDeleteHook implements IMoveDeleteHook {
	private static final boolean I_AM_DONE = true;

	private static final boolean FINISH_FOR_ME = false;

	private final GitProjectData data;

	GitMoveDeleteHook(final GitProjectData d) {
		Assert.isNotNull(d);
		data = d;
	}

	public boolean deleteFile(final IResourceTree tree, final IFile file,
			final int updateFlags, final IProgressMonitor monitor) {
		final boolean force = (updateFlags & IResource.FORCE) == IResource.FORCE;
		if (!force && !tree.isSynchronized(file, IResource.DEPTH_ZERO))
			return false;

		final RepositoryMapping map = RepositoryMapping.getMapping(file);
		if (map == null)
			return false;

		try {
			final DirCache dirc = DirCache.lock(map.getRepository());
			final int first = dirc.findEntry(map.getRepoRelativePath(file));
			if (first < 0) {
				dirc.unlock();
				return false;
			}

			final DirCacheBuilder edit = dirc.builder();
			if (first > 0)
				edit.keep(0, first);
			final int next = dirc.nextEntry(first);
			if (next < dirc.getEntryCount())
				edit.keep(next, dirc.getEntryCount() - next);
			if (!edit.commit())
				tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
						0, CoreText.MoveDeleteHook_operationError, null));
			tree.standardDeleteFile(file, updateFlags, monitor);
		} catch (IOException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
		}
		return true;
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

	public boolean moveFile(final IResourceTree tree, final IFile srcf,
			final IFile dstf, final int updateFlags,
			final IProgressMonitor monitor) {
		final boolean force = (updateFlags & IResource.FORCE) == IResource.FORCE;
		if (!force && !tree.isSynchronized(srcf, IResource.DEPTH_ZERO))
			return false;

		final RepositoryMapping srcm = RepositoryMapping.getMapping(srcf);
		if (srcm == null)
			return false;
		final RepositoryMapping dstm = RepositoryMapping.getMapping(dstf);

		try {
			final DirCache sCache = DirCache.lock(srcm.getRepository());
			final String sPath = srcm.getRepoRelativePath(srcf);
			final DirCacheEntry sEnt = sCache.getEntry(sPath);
			if (sEnt == null) {
				sCache.unlock();
				return false;
			}

			final DirCacheEditor sEdit = sCache.editor();
			sEdit.add(new DirCacheEditor.DeletePath(sEnt));
			if (dstm != null && dstm.getRepository() == srcm.getRepository()) {
				final String dPath = srcm.getRepoRelativePath(dstf);
				sEdit.add(new DirCacheEditor.PathEdit(dPath) {
					@Override
					public void apply(final DirCacheEntry dEnt) {
						dEnt.copyMetaData(sEnt);
					}
				});
			}
			if (!sEdit.commit())
				tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
						0, CoreText.MoveDeleteHook_operationError, null));

			tree.standardMoveFile(srcf, dstf, updateFlags, monitor);
		} catch (IOException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
		}
		return true;
	}

	public boolean moveFolder(final IResourceTree tree, final IFolder srcf,
			final IFolder dstf, final int updateFlags,
			final IProgressMonitor monitor) {
		final boolean force = (updateFlags & IResource.FORCE) == IResource.FORCE;
		if (!force && !tree.isSynchronized(srcf, IResource.DEPTH_ZERO))
			return false;

		final RepositoryMapping srcm = RepositoryMapping.getMapping(srcf);
		if (srcm == null)
			return false;
		final RepositoryMapping dstm = RepositoryMapping.getMapping(dstf);

		try {
			final DirCache sCache = DirCache.lock(srcm.getRepository());
			final String sPath = srcm.getRepoRelativePath(srcf);
			final DirCacheEntry[] sEnt = sCache.getEntriesWithin(sPath);
			if (sEnt.length == 0) {
				sCache.unlock();
				return false;
			}

			final DirCacheEditor sEdit = sCache.editor();
			sEdit.add(new DirCacheEditor.DeleteTree(sPath));
			if (dstm != null && dstm.getRepository() == srcm.getRepository()) {
				final String dPath = srcm.getRepoRelativePath(dstf) + "/";
				final int sPathLen = sPath.length() + 1;
				for (final DirCacheEntry se : sEnt) {
					final String p = se.getPathString().substring(sPathLen);
					sEdit.add(new DirCacheEditor.PathEdit(dPath + p) {
						@Override
						public void apply(final DirCacheEntry dEnt) {
							dEnt.copyMetaData(se);
						}
					});
				}
			}
			if (!sEdit.commit())
				tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
						0, CoreText.MoveDeleteHook_operationError, null));

			tree.standardMoveFolder(srcf, dstf, updateFlags, monitor);
		} catch (IOException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
		}
		return true;
	}

	public boolean moveProject(final IResourceTree tree, final IProject source,
			final IProjectDescription description, final int updateFlags,
			final IProgressMonitor monitor) {
		// TODO: We should be able to do this without too much effort when the
		// projects belong to the same Git repository.
		return FINISH_FOR_ME;
	}

	private boolean cannotModifyRepository(final IResourceTree tree) {
		tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
				CoreText.MoveDeleteHook_cannotModifyFolder, null));
		return I_AM_DONE;
	}
}
