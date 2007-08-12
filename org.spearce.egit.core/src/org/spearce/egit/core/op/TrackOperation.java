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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.Team;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.GitIndex;

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
 * Resources are only scheduled for addition in the index.
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

		final IdentityHashMap<RepositoryMapping, Boolean> tomerge = new IdentityHashMap<RepositoryMapping, Boolean>();
		m.beginTask(CoreText.AddOperation_adding, rsrcList.size() * 200);
		try {
			final Iterator i = rsrcList.iterator();
			while (i.hasNext()) {
				final Object obj = i.next();
				if (obj instanceof IResource) {
					final IResource toAdd = (IResource)obj;
					final RepositoryMapping rm = RepositoryMapping.getMapping(toAdd);
					final GitIndex index = rm.getRepository().getIndex();

					if (obj instanceof IFile) {
						String repoPath = rm.getRepoRelativePath((IResource) obj);
						if (index.getEntry(repoPath) != null) {
							System.out.println("Already tracked - skipping");
							continue;
						}
					}

					tomerge.put(rm, Boolean.TRUE);
					if (toAdd instanceof IContainer) {
						((IContainer)toAdd).accept(new IResourceVisitor() {
							public boolean visit(IResource resource) throws CoreException {
								try {
									if (resource.getType() == IResource.FILE) {
										if (!Team.isIgnored((IFile)resource))
											index.add(rm.getWorkDir(), new File(rm.getWorkDir(),rm.getRepoRelativePath(resource)));
									}
								} catch (IOException e) {
									e.printStackTrace();
									throw Activator.error(CoreText.AddOperation_failed, e);
								}
								return true;
							}
						},IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
					} else {
						index.add(rm.getWorkDir(), new File(rm.getWorkDir(),rm.getRepoRelativePath(toAdd)));
						
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
			throw Activator.error(CoreText.AddOperation_failed, e);
		} catch (IOException e) {
			e.printStackTrace();
			throw Activator.error(CoreText.AddOperation_failed, e);
		} finally {
			try {
				final Iterator i = tomerge.keySet().iterator();
				while (i.hasNext()) {
					final RepositoryMapping r = (RepositoryMapping) i.next();
					r.getRepository().getIndex().read();
					r.recomputeMerge();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				m.done();
			}
		}
	}
}
