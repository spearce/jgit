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

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;

public class DisconnectProviderOperation implements IWorkspaceRunnable {
	private final Collection projectList;

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
			final Iterator i = projectList.iterator();
			while (i.hasNext()) {
				final Object obj = i.next();
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
