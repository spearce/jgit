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
import org.eclipse.swt.widgets.Display;
import org.spearce.egit.core.op.BranchOperation;
import org.spearce.egit.ui.internal.decorators.GitResourceDecorator;
import org.spearce.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.spearce.jgit.lib.Repository;

public class BranchAction extends RepositoryAction {
	// There are changes in Eclipse 3.3 requiring that execute be implemented
	// for it to compile. while 3.2 requires that run is implemented instead.
	public void execute(IAction action) {
		run(action);
	}

	
	@Override
	public void run(IAction action) {
		final Repository repository = getRepository();
		if (repository == null)
			return;

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
						GitResourceDecorator.refresh();
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
}
