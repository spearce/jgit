/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.spearce.egit.core.op.BranchOperation;
import org.spearce.egit.ui.internal.decorators.GitLightweightDecorator;
import org.spearce.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.spearce.jgit.lib.Repository;

/**
 * Action for selecting a branch and checking it out.
 *
 * @see BranchOperation
 */
public class BranchAction extends RepositoryAction {
	@Override
	public void run(IAction action) {
		final Repository repository = getRepository(true);
		if (repository == null)
			return;

		if (!repository.getRepositoryState().canCheckout()) {
			MessageDialog.openError(getShell(), "Cannot checkout now",
					"Repository state:"
							+ repository.getRepositoryState().getDescription());
			return;
		}

		BranchSelectionDialog dialog = new BranchSelectionDialog(getShell(), repository);
		dialog.setShowResetType(false);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}
		
		final String refName = dialog.getRefName();
		try {
			getTargetPart().getSite().getWorkbenchWindow().run(true, false,
					new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor)
				throws InvocationTargetException {
					try {
						new BranchOperation(repository, refName).run(monitor);
						GitLightweightDecorator.refresh();
					} catch (final CoreException ce) {
						ce.printStackTrace();
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								handle(ce, "Error while switching branches", "Unable to switch branches");							
							}
						});
					}
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isEnabled() {
		return getRepository(false) != null;
	}
}
