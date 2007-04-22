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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
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
 * Add one or more new files/folders to the Git repository.
 * <p>
 * Accepts a collection of resources (files and/or directories) which should be
 * added to the their corresponding Git repositories. Resources in the
 * collection can be associated with multiple repositories. The operation will
 * automatically associate each resource with the nearest containing Git
 * repository.
 * </p>
 * <p>
 * Resources are only scheduled for addition in the cache-tree. Their backing
 * object in the object database is not built yet (as that can be a time
 * consuming operation, depending on file size) and the cache-tree will be dirty
 * in memory, needing a checkpoint.
 * </p>
 */
public class TrackOperation implements IWorkspaceRunnable {
	private final Collection rsrcList;

	/**
	 * Create a new operation to track additional files/folders.
	 * 
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be added to the
	 *            relevant Git repositories.
	 */
	public TrackOperation(final Collection rsrcs) {
		rsrcList = rsrcs;
	}

	public void run(IProgressMonitor m) throws CoreException {
		if (m == null) {
			m = new NullProgressMonitor();
		}

		final IdentityHashMap tomerge = new IdentityHashMap();
		m.beginTask(CoreText.AddOperation_adding, rsrcList.size() * 200);
		try {
			final Iterator i = rsrcList.iterator();
			while (i.hasNext()) {
				final Object obj = i.next();
				if (obj instanceof IResource) {
					add(tomerge, (IResource) obj);
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
				throw Activator.error(CoreText.AddOperation_failed, ioe);
			} finally {
				m.done();
			}
		}
	}

	private void add(final Map tomerge, final IResource toAdd)
			throws CoreException {
		final GitProjectData pd = GitProjectData.get(toAdd.getProject());
		IResource r = toAdd;
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
			tomerge.put(m, m);
			add(m.getCacheTree(), s, toAdd);
		} catch (IOException ioe) {
			throw Activator.error(CoreText.AddOperation_failed, ioe);
		}
	}

	private void add(final Tree t, final String path, final IResource toAdd)
			throws IOException, CoreException {
		if (!toAdd.exists()) {
			// Uh, what? Why are we adding a phantom resource? Just say no!
			//
		} else if (toAdd instanceof IFile) {
			if (!t.existsTree(path)) {
				if (!t.existsBlob(path)) {
					t.addFile(path);
				}
			}
		} else if (toAdd instanceof IContainer) {
			final IResource[] m = ((IContainer) toAdd).members();
			final TreeEntry e = t.findTreeMember(path);
			final Tree c;
			c = e instanceof Tree ? (Tree) e : e == null ? t.addTree(path)
					: null;
			if (c != null) {
				for (int k = 0; k < m.length; k++) {
					add(c, m[k].getName(), m[k]);
				}

				// GIT does not take kindly to empty trees. If we just created
				// such a thing remove it. We do the detection after-the-fact
				// as its hard to know if all of our children were also empty
				// subtrees.
				//
				if (c.memberCount() == 0) {
					c.delete();
				}
			}
		}
	}
}
