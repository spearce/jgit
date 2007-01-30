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
package org.spearce.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryChangeListener;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.MergedTree;
import org.spearce.jgit.lib.TreeEntry;

public class GitResourceDecorator extends LabelProvider implements
		ILightweightLabelDecorator {

	private static final RepositoryChangeListener myrcl = new RCL();

	static class RCL implements RepositoryChangeListener, Runnable {
		private boolean requested;

		public synchronized void run() {
			requested = false;
			refresh();
		}

		public synchronized void repositoryChanged(final RepositoryMapping which) {
			if (!requested) {
				final Display d = PlatformUI.getWorkbench().getDisplay();
				if (d.getThread() == Thread.currentThread()) {
					run();
				} else {
					requested = true;
					d.asyncExec(this);
				}
			}
		}
	};

	static {
		GitProjectData.addRepositoryChangeListener(myrcl);
	}

	public static void refresh() {
		PlatformUI.getWorkbench().getDecoratorManager().update(
				GitResourceDecorator.class.getName());
	}

	private static IResource toIResource(final Object e) {
		if (e instanceof IResource) {
			return (IResource) e;
		} else if (e instanceof IAdaptable) {
			final Object c = ((IAdaptable) e).getAdapter(IResource.class);
			if (c instanceof IResource) {
				return (IResource) c;
			}
		}
		return null;
	}

	public void decorate(final Object element, final IDecoration decoration) {
		final IResource rsrc = toIResource(element);
		if (rsrc == null) {
			return;
		}

		final GitProjectData d = GitProjectData.get(rsrc.getProject());
		if (d == null) {
			return;
		}

		RepositoryMapping mapped = d.getRepositoryMapping(rsrc);
		if (mapped != null) {
			if (mapped.getRepository().isStGitMode())
				decoration.addSuffix(" [StGit]");
			else
				decoration.addSuffix(" [Git]");
			decoration.addOverlay(UIIcons.OVR_SHARED);
			return;
		}

		TreeEntry[] n;
		try {
			n = d.getActiveDiffTreeEntries(rsrc);
		} catch (CoreException ioe) {
			// If we throw an exception Eclipse will log the error and
			// unregister us thereby preventing us from dragging down the
			// entire workbench because we are crashing.
			//
			throw new RuntimeException(UIText.Decorator_failedLazyLoading, ioe);
		}

		if (n != null) {
			if (MergedTree.isAdded(n)) {
				decoration.addOverlay(UIIcons.OVR_PENDING_ADD);
			} else if (MergedTree.isRemoved(n)) {
				decoration.addOverlay(UIIcons.OVR_PENDING_REMOVE);
			} else if (MergedTree.isModified(n)) {
				decoration.addPrefix(">");
				decoration.addOverlay(UIIcons.OVR_SHARED);
			} else {
				decoration.addOverlay(UIIcons.OVR_SHARED);
			}
		} else {
			decoration.addSuffix(" (untracked)");
		}
	}
}
