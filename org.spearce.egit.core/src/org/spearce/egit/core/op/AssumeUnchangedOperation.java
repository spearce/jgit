/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.GitIndex.Entry;

/**
 * Tell JGit to ignore changes in selected files
 */
public class AssumeUnchangedOperation implements IWorkspaceRunnable {
	private final Collection rsrcList;

	/**
	 * Create a new operation to ignore changes in tracked files
	 *
	 * @param rsrcs
	 *            collection of {@link IResource}s which should be 
	 *            ignored when looking for changes or committing.
	 */
	public AssumeUnchangedOperation(final Collection rsrcs) {
		rsrcList = rsrcs;
	}

	public void run(IProgressMonitor m) throws CoreException {
		if (m == null) {
			m = new NullProgressMonitor();
		}

		final IdentityHashMap<RepositoryMapping, Boolean> tomerge = new IdentityHashMap<RepositoryMapping, Boolean>();
		m.beginTask(CoreText.AssumeUnchangedOperation_adding, rsrcList.size() * 200);
		try {
			for (Object obj : rsrcList) {
				obj = ((IAdaptable)obj).getAdapter(IResource.class);
				if (obj instanceof IResource) {
					final IResource toAssumeValid = (IResource)obj;
					final RepositoryMapping rm = RepositoryMapping.getMapping(toAssumeValid);
					final GitIndex index = rm.getRepository().getIndex();

					if (toAssumeValid instanceof IContainer) {
						((IContainer)toAssumeValid).accept(new IResourceVisitor() {
							public boolean visit(IResource resource) throws CoreException {
								try {
									String repoPath = rm.getRepoRelativePath(resource);
									Entry entry = index.getEntry(repoPath);
									if (entry != null) {
										if (!entry.isAssumedValid()) {
											entry.setAssumeValid(true);
											tomerge.put(rm, Boolean.TRUE);
										}
									}
								} catch (IOException e) {
									e.printStackTrace();
									throw Activator.error(CoreText.AssumeUnchangedOperation_failed, e);
								}
								return true;
							}
						},IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
					} else {
						String repoPath = rm.getRepoRelativePath((IResource) obj);
						Entry entry = index.getEntry(repoPath);
						if (entry != null) {
							if (!entry.isAssumedValid()) {
								entry.setAssumeValid(true);
								tomerge.put(rm, Boolean.TRUE);
							}
						}
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
			throw Activator.error(CoreText.AssumeUnchangedOperation_failed, e);
		} catch (IOException e) {
			e.printStackTrace();
			throw Activator.error(CoreText.AssumeUnchangedOperation_failed, e);
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
