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
package org.spearce.egit.ui.internal.sharing;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;
import org.spearce.egit.core.op.ConnectProviderOperation;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;

/**
 * The dialog used for activating Team>Share, i.e. to create a new
 * Git repository or associate a project with one.
 */
public class SharingWizard extends Wizard implements IConfigurationWizard {
	private IProject project;

	private boolean create;

	private File newGitDir;

	/**
	 * Construct the Git Sharing Wizard for connecting Git project to Eclipse
	 */
	public SharingWizard() {
		setWindowTitle(UIText.SharingWizard_windowTitle);
		setNeedsProgressMonitor(true);
	}

	public void init(final IWorkbench workbench, final IProject p) {
		project = p;
		newGitDir = new File(project.getLocation().toFile(), ".git");
	}

	public void addPages() {
		addPage(new ExistingOrNewPage(this));
	}

	boolean canCreateNew() {
		return !newGitDir.exists();
	}

	void setCreateNew() {
		if (canCreateNew()) {
			create = true;
		}
	}

	void setUseExisting() {
		create = false;
	}

	public boolean performFinish() {
		final ConnectProviderOperation op = new ConnectProviderOperation(
				project, create ? newGitDir : null);
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException {
					try {
						op.run(monitor);
					} catch (CoreException ce) {
						throw new InvocationTargetException(ce);
					}
				}
			});
			return true;
		} catch (Throwable e) {
			if (e instanceof InvocationTargetException) {
				e = e.getCause();
			}
			final IStatus status;
			if (e instanceof CoreException) {
				status = ((CoreException) e).getStatus();
				e = status.getException();
			} else {
				status = new Status(IStatus.ERROR, Activator.getPluginId(), 1,
						UIText.SharingWizard_failed, e);
			}
			Activator.logError(UIText.SharingWizard_failed, e);
			ErrorDialog.openError(getContainer().getShell(), getWindowTitle(),
					UIText.SharingWizard_failed, status, status.getSeverity());
			return false;
		}
	}
}
