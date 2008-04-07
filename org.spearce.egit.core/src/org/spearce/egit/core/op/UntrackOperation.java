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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.GitIndex;

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

		final IdentityHashMap<RepositoryMapping, Boolean> tomerge = new IdentityHashMap<RepositoryMapping, Boolean>();
		m.beginTask(CoreText.AddOperation_adding, rsrcList.size() * 200);
		try {
			final Iterator i = rsrcList.iterator();
			while (i.hasNext()) {
				final Object obj = i.next();
				if (obj instanceof IResource) {
					final IResource toRemove = (IResource)obj;
					final IProject p = toRemove.getProject();					
					final GitProjectData pd = GitProjectData.get(toRemove.getProject());
					final RepositoryMapping rm = pd.getRepositoryMapping(p);
					final GitIndex index = rm.getRepository().getIndex();
					tomerge.put(rm, Boolean.TRUE);
					if (toRemove instanceof IContainer) {
						((IContainer)toRemove).accept(new IResourceVisitor() {
							public boolean visit(IResource resource) throws CoreException {
								if (resource.getType() == IResource.FILE) {
									index.remove(rm.getWorkDir(), new File(rm.getWorkDir(),rm.getRepoRelativePath(resource)));
								}
								return true;
							}
						},IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
					} else {
						index.remove(rm.getWorkDir(), new File(rm.getWorkDir(),rm.getRepoRelativePath(toRemove)));
					}
				}
				m.worked(200);
			}
			for (RepositoryMapping rm : tomerge.keySet()) {
				m.setTaskName("Writing index for "+rm.getRepository().getDirectory());
				rm.getRepository().getIndex().write();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw Activator.error(CoreText.UntrackOperation_failed, e);
		} catch (IOException e) {
			e.printStackTrace();
			throw Activator.error(CoreText.UntrackOperation_failed, e);
		} finally {
			try {
				final Iterator i = tomerge.keySet().iterator();
				while (i.hasNext()) {
					final RepositoryMapping r = (RepositoryMapping) i.next();
					r.getRepository().getIndex().read();
					r.fireRepositoryChanged();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				m.done();
			}
		}
	}

}
