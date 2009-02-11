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
import org.spearce.egit.core.op.ResetOperation;
import org.spearce.egit.core.op.ResetOperation.ResetType;
import org.spearce.egit.ui.internal.decorators.GitLightweightDecorator;
import org.spearce.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.spearce.jgit.lib.Repository;

/**
 * An action to reset the current branch to a specific revision.
 *
 * @see ResetOperation
 */
public class ResetAction extends RepositoryAction {

	@Override
	public void run(IAction action) {
		final Repository repository = getRepository(true);
		if (repository == null)
			return;
		
		if (!repository.getRepositoryState().canResetHead()) {
			MessageDialog.openError(getShell(), "Cannot reset HEAD now",
					"Repository state:"
							+ repository.getRepositoryState().getDescription());
			return;
		}

		BranchSelectionDialog branchSelectionDialog = new BranchSelectionDialog(getShell(), repository);
		if (branchSelectionDialog.open() == IDialogConstants.OK_ID) {
			final String refName = branchSelectionDialog.getRefName();
			final ResetType type = branchSelectionDialog.getResetType();

			try {
				getTargetPart().getSite().getWorkbenchWindow().run(true, false,
						new IRunnableWithProgress() {
					public void run(final IProgressMonitor monitor)
					throws InvocationTargetException {
						try {
							new ResetOperation(repository, refName, type).run(monitor);
							GitLightweightDecorator.refresh();
						} catch (CoreException ce) {
							ce.printStackTrace();
							throw new InvocationTargetException(ce);
						}
					}
				});
			} catch (InvocationTargetException e) {
				MessageDialog.openError(getShell(),"Reset failed", e.getMessage());
			} catch (InterruptedException e) {
				MessageDialog.openError(getShell(),"Reset failed", e.getMessage());
			}
		}
		
	}

	@Override
	public boolean isEnabled() {
		return getRepository(false) != null;
	}
}
