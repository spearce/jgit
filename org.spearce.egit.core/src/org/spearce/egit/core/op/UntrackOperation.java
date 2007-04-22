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
package org.spearce.egit.core.op;

import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

/**
 * Remove one or more existing files/folders from the Git repository.
 * <p>
 * Accepts a collection of resources (files and/or directories) which should be
 * removed from the their corresponding Git repositories. Resources in the
 * collection can be associated with multiple repositories. The operation will
 * automatically remove each resource from the correct Git repository.
 * </p>
 * <p>
 * Resources are only scheduled for removal in the cache-tree. The cache-tree
 * will be dirty in memory, needing a checkpoint.
 * </p>
 */
public class UntrackOperation implements IWorkspaceRunnable {
	private final Collection rsrcList;

	/**
	 * Create a new operation to stop tracking existing files/folders.
	 * 
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be removed from
	 *            the relevant Git repositories.
	 */
	public UntrackOperation(final Collection rsrcs) {
		rsrcList = rsrcs;
	}

	public void run(IProgressMonitor m) throws CoreException {
		if (m == null) {
			m = new NullProgressMonitor();
		}

		final IdentityHashMap tomerge = new IdentityHashMap();
		m.beginTask(CoreText.UntrackOperation_adding, rsrcList.size() * 200);
		try {
			final Iterator i = rsrcList.iterator();
			while (i.hasNext()) {
				final Object obj = i.next();
				if (obj instanceof IResource) {
					untrack(tomerge, (IResource) obj);
				}
				m.worked(200);
			}
		} finally {
			try {
				final Iterator i = tomerge.keySet().iterator();
				while (i.hasNext()) {
					final RepositoryMapping r = (RepositoryMapping) i.next();
					r.recomputeMerge();
				}
			} catch (IOException ioe) {
				throw Activator.error(CoreText.UntrackOperation_failed, ioe);
			} finally {
				m.done();
			}
		}
	}

	private void untrack(final Map tomerge, final IResource torm)
			throws CoreException {
		final GitProjectData pd = GitProjectData.get(torm.getProject());
		IResource r = torm;
		String s = null;
		RepositoryMapping m = null;

		while (r != null) {
			m = pd.getRepositoryMapping(r);
			if (m != null) {
				break;
			}

			if (s != null) {
				s = r.getName() + "/" + s;
			} else {
				s = r.getName();
			}

			r = r.getParent();
		}

		if (s == null || m == null || m.getCacheTree() == null) {
			return;
		}

		try {
			final Tree t = m.getCacheTree();
			final TreeEntry e;
			if (torm.getType() == IResource.FILE)
				e = t.findBlobMember(s);
			else
				e = t.findTreeMember(s);
			if (e != null) {
				e.delete();
				tomerge.put(m, m);
			}
		} catch (IOException ioe) {
			throw Activator.error(CoreText.UntrackOperation_failed, ioe);
		}
	}
}
