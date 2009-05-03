/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.Team;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.GitIndex.Entry;

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
			for (Object obj : rsrcList) {
				obj = ((IAdaptable)obj).getAdapter(IResource.class);
				if (obj instanceof IResource) {
					final IResource toAdd = (IResource)obj;
					final RepositoryMapping rm = RepositoryMapping.getMapping(toAdd);
					final GitIndex index = rm.getRepository().getIndex();

					if (obj instanceof IFile) {
						String repoPath = rm.getRepoRelativePath((IResource) obj);
						Entry entry = index.getEntry(repoPath);
						if (entry != null) {
							if (!entry.isAssumedValid()) {
								System.out.println("Already tracked - skipping");
								continue;
							}
						}
					}

					tomerge.put(rm, Boolean.TRUE);
					if (toAdd instanceof IContainer) {
						((IContainer)toAdd).accept(new IResourceVisitor() {
							public boolean visit(IResource resource) throws CoreException {
								try {
									String repoPath = rm.getRepoRelativePath(resource);
									// We use add to reset the assume valid bit, so we check the bit
									// first. If a resource within a ignored folder is marked
									// we ignore it here, i.e. there is no way to unmark it expect
									// by explicitly selecting and invoking track on it.
									boolean isIgnored = Team.isIgnoredHint(resource);
									if (resource.getType() == IResource.FILE) {
										Entry entry = index.getEntry(repoPath);
										if (!isIgnored || entry != null && entry.isAssumedValid()) {
											entry = index.add(rm.getWorkDir(), new File(rm.getWorkDir(), repoPath));
											entry.setAssumeValid(false);
										}
									}
									if (isIgnored)
										return false;

								} catch (IOException e) {
									e.printStackTrace();
									throw Activator.error(CoreText.AddOperation_failed, e);
								}
								return true;
							}
						},IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
					} else {
						Entry entry = index.add(rm.getWorkDir(), new File(rm.getWorkDir(),rm.getRepoRelativePath(toAdd)));
						entry.setAssumeValid(false);
						
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
