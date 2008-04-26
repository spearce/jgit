/*
 *  Copyright (C) 2007 Dave Watson <dwatson@mimvista.com>
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
import org.spearce.egit.ui.internal.decorators.GitResourceDecorator;
import org.spearce.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.spearce.jgit.lib.Repository;

/**
 * An acton to reset the current branch to a specific revision.
 *
 * @see ResetOperation
 */
public class ResetAction extends RepositoryAction {

	@Override
	public void run(IAction action) {
		final Repository repository = getRepository();
		if (repository == null)
			return;
		
		if (!repository.getRepositoryState().canResetHead()) {
			MessageDialog.openError(getShell(), "Cannot reset HEAD now",
					"Respository state:"
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
							GitResourceDecorator.refresh();
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
		return !getSelection().isEmpty();
	}
}
