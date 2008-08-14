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
import org.spearce.jgit.dircache.DirCacheEntry;
import org.spearce.jgit.lib.Repository;

/**
 * Tell JGit to ignore changes in selected files
 */
public class AssumeUnchangedOperation implements IWorkspaceRunnable {
	private final Collection rsrcList;

	private final IdentityHashMap<Repository, DirCache> caches;

	private final IdentityHashMap<RepositoryMapping, Object> mappings;

	/**
	 * Create a new operation to ignore changes in tracked files
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be ignored when
	 *            looking for changes or committing.
	 */
	public AssumeUnchangedOperation(final Collection rsrcs) {
		rsrcList = rsrcs;
		caches = new IdentityHashMap<Repository, DirCache>();
		mappings = new IdentityHashMap<RepositoryMapping, Object>();
	}

	public void run(IProgressMonitor m) throws CoreException {
		if (m == null)
			m = new NullProgressMonitor();

		caches.clear();
		mappings.clear();

		m.beginTask(CoreText.AssumeUnchangedOperation_adding,
				rsrcList.size() * 200);
		try {
			for (Object obj : rsrcList) {
				obj = ((IAdaptable) obj).getAdapter(IResource.class);
				if (obj instanceof IResource)
					assumeValid((IResource) obj);
				m.worked(200);
			}

			for (Map.Entry<Repository, DirCache> e : caches.entrySet()) {
				final Repository db = e.getKey();
				final DirCache editor = e.getValue();
				m.setTaskName("Writing index for " + db.getDirectory());
				editor.write();
				editor.commit();
			}
		} catch (RuntimeException e) {
			throw Activator.error(CoreText.UntrackOperation_failed, e);
		} catch (IOException e) {
			throw Activator.error(CoreText.UntrackOperation_failed, e);
		} finally {
			for (final RepositoryMapping rm : mappings.keySet())
				rm.fireRepositoryChanged();
			caches.clear();
			mappings.clear();
			m.done();
		}
	}

	private void assumeValid(final IResource resource) throws CoreException {
		final IProject proj = resource.getProject();
		final GitProjectData pd = GitProjectData.get(proj);
		if (pd == null)
			return;
		final RepositoryMapping rm = pd.getRepositoryMapping(resource);
		if (rm == null)
			return;
		final Repository db = rm.getRepository();

		DirCache cache = caches.get(db);
		if (cache == null) {
			try {
				cache = DirCache.lock(db);
			} catch (IOException err) {
				throw Activator.error(CoreText.UntrackOperation_failed, err);
			}
			caches.put(db, cache);
			mappings.put(rm, rm);
		}

		final String path = rm.getRepoRelativePath(resource);
		if (resource instanceof IContainer) {
			for (final DirCacheEntry ent : cache.getEntriesWithin(path))
				ent.setAssumeValid(true);
		} else {
			final DirCacheEntry ent = cache.getEntry(path);
			if (ent != null)
				ent.setAssumeValid(true);
		}
	}
}
