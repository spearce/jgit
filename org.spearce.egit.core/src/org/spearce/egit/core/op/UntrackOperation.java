/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.dircache.DirCache;
import org.spearce.jgit.dircache.DirCacheEditor;
import org.spearce.jgit.lib.Repository;

/**
 * Remove one or more existing files/folders from the Git repository.
 * <p>
 * Accepts a collection of resources (files and/or directories) which should be
 * removed from the their corresponding Git repositories. Resources in the
 * collection can be associated with multiple repositories. The operation will
 * automatically remove each resource from the correct Git repository.
 * </p>
 * <p>
 * Resources are only scheduled for removal in the index-
 * </p>
 */
public class UntrackOperation implements IWorkspaceRunnable {
	private final Collection rsrcList;

	private final IdentityHashMap<Repository, DirCacheEditor> edits;

	private final IdentityHashMap<RepositoryMapping, Object> mappings;

	/**
	 * Create a new operation to stop tracking existing files/folders.
	 * 
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be removed from
	 *            the relevant Git repositories.
	 */
	public UntrackOperation(final Collection rsrcs) {
		rsrcList = rsrcs;
		edits = new IdentityHashMap<Repository, DirCacheEditor>();
		mappings = new IdentityHashMap<RepositoryMapping, Object>();
	}

	public void run(IProgressMonitor m) throws CoreException {
		if (m == null)
			m = new NullProgressMonitor();

		edits.clear();
		mappings.clear();

		m.beginTask(CoreText.AddOperation_adding, rsrcList.size() * 200);
		try {
			for (Object obj : rsrcList) {
				obj = ((IAdaptable) obj).getAdapter(IResource.class);
				if (obj instanceof IResource)
					remove((IResource) obj);
				m.worked(200);
			}

			for (Map.Entry<Repository, DirCacheEditor> e : edits.entrySet()) {
				final Repository db = e.getKey();
				final DirCacheEditor editor = e.getValue();
				m.setTaskName("Writing index for " + db.getDirectory());
				editor.commit();
			}
		} catch (RuntimeException e) {
			throw Activator.error(CoreText.UntrackOperation_failed, e);
		} catch (IOException e) {
			throw Activator.error(CoreText.UntrackOperation_failed, e);
		} finally {
			for (final RepositoryMapping rm : mappings.keySet())
				rm.fireRepositoryChanged();
			edits.clear();
			mappings.clear();
			m.done();
		}
	}

	private void remove(final IResource path) throws CoreException {
		final IProject proj = path.getProject();
		final GitProjectData pd = GitProjectData.get(proj);
		if (pd == null)
			return;
		final RepositoryMapping rm = pd.getRepositoryMapping(path);
		if (rm == null)
			return;
		final Repository db = rm.getRepository();

		DirCacheEditor e = edits.get(db);
		if (e == null) {
			try {
				e = DirCache.lock(db).editor();
			} catch (IOException err) {
				throw Activator.error(CoreText.UntrackOperation_failed, err);
			}
			edits.put(db, e);
			mappings.put(rm, rm);
		}

		if (path instanceof IContainer)
			e.add(new DirCacheEditor.DeleteTree(rm.getRepoRelativePath(path)));
		else
			e.add(new DirCacheEditor.DeletePath(rm.getRepoRelativePath(path)));
	}
}
