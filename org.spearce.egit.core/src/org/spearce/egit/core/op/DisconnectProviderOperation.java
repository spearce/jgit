/*******************************************************************************
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;

/**
 * Disconnects the Git team provider from a project.
 * <p>
 * Once disconnected, Git operations will no longer be available on the project.
 * </p>
 */
public class DisconnectProviderOperation implements IWorkspaceRunnable {
	private final Collection projectList;

	/**
	 * Create a new disconnect operation.
	 * 
	 * @param projs
	 *            the collection of {@link IProject}s which should be
	 *            disconnected from the Git team provider, and returned to
	 *            untracked/unmanaged status.
	 */
	public DisconnectProviderOperation(final Collection projs) {
		projectList = projs;
	}

	public void run(IProgressMonitor m) throws CoreException {
		if (m == null) {
			m = new NullProgressMonitor();
		}

		m.beginTask(CoreText.DisconnectProviderOperation_disconnecting,
				projectList.size() * 200);
		try {
			for (Object obj : projectList) {
				obj = ((IAdaptable)obj).getAdapter(IResource.class);
				if (obj instanceof IProject) {
					final IProject p = (IProject) obj;

					Activator.trace("disconnect " + p.getName());
					unmarkTeamPrivate(p);
					RepositoryProvider.unmap(p);
					m.worked(100);

					p.refreshLocal(IResource.DEPTH_INFINITE,
							new SubProgressMonitor(m, 100));
				} else {
					m.worked(200);
				}
			}
		} finally {
			m.done();
		}
	}

	private void unmarkTeamPrivate(final IContainer p) throws CoreException {
		final IResource[] c;
		c = p.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		if (c != null) {
			for (int k = 0; k < c.length; k++) {
				if (c[k] instanceof IContainer) {
					unmarkTeamPrivate((IContainer) c[k]);
				}
				if (c[k].isTeamPrivateMember()) {
					Activator.trace("notTeamPrivate " + c[k]);
					c[k].setTeamPrivateMember(false);
				}
			}
		}
	}
}
