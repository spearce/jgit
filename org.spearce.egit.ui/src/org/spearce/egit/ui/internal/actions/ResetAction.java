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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.spearce.egit.core.op.ResetOperation;
import org.spearce.egit.core.op.ResetOperation.ResetType;
import org.spearce.egit.ui.internal.decorators.GitResourceDecorator;
import org.spearce.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.spearce.jgit.lib.Repository;

public class ResetAction extends RepositoryAction {
	@Override
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		final Repository repository = getRepository();
		if (repository == null)
			return;
		
		BranchSelectionDialog branchSelectionDialog = new BranchSelectionDialog(getShell(), repository);
		if (branchSelectionDialog.open() == IDialogConstants.OK_ID) {
			final String refName = branchSelectionDialog.getRefName();
			final ResetType type = branchSelectionDialog.getResetType();

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
		}
		
	}
}
