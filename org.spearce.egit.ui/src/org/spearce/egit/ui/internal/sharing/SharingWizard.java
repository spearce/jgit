/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.sharing;

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
import org.eclipse.team.ui.IConfigurationWizardExtension;
import org.eclipse.ui.IWorkbench;
import org.spearce.egit.core.op.ConnectProviderOperation;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;

/**
 * The dialog used for activating Team>Share, i.e. to create a new
 * Git repository or associate projects with one.
 */
public class SharingWizard extends Wizard implements IConfigurationWizard,
		IConfigurationWizardExtension {
	IProject[] projects;

	private ExistingOrNewPage existingPage;

	/**
	 * Construct the Git Sharing Wizard for connecting Git project to Eclipse
	 */
	public SharingWizard() {
		setWindowTitle(UIText.SharingWizard_windowTitle);
		setNeedsProgressMonitor(true);
	}

	public void init(IWorkbench workbench, IProject[] p) {
		this.projects = new IProject[p.length];
		System.arraycopy(p, 0, this.projects, 0, p.length);
	}

	public void init(final IWorkbench workbench, final IProject p) {
		projects = new IProject[] { p };
	}

	public void addPages() {
		existingPage = new ExistingOrNewPage(this);
		addPage(existingPage);
	}

	public boolean performFinish() {
		final ConnectProviderOperation op = new ConnectProviderOperation(
				existingPage.getProjects());
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

	@Override
	public boolean canFinish() {
		return existingPage.isPageComplete();
	}
}
