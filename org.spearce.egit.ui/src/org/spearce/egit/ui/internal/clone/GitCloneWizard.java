/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.clone;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.spearce.egit.core.op.CloneOperation;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.components.RepositorySelectionPage;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.transport.URIish;

/**
 * Import Git Repository Wizard. A front end to a git clone operation.
 */
public class GitCloneWizard extends Wizard implements IImportWizard {
	private RepositorySelectionPage cloneSource;

	private SourceBranchPage validSource;

	private CloneDestinationPage cloneDestination;

	private GitProjectsImportPage importProject;

	public void init(IWorkbench arg0, IStructuredSelection arg1) {
		setWindowTitle(UIText.GitCloneWizard_title);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setNeedsProgressMonitor(true);
		cloneSource = new RepositorySelectionPage(true);
		validSource = new SourceBranchPage(cloneSource);
		cloneDestination = new CloneDestinationPage(cloneSource, validSource);
		importProject = new GitProjectsImportPage() {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					if (cloneDestination.alreadyClonedInto == null) {
						performClone(false);
						cloneDestination.alreadyClonedInto = cloneDestination
								.getDestinationFile().getAbsolutePath();
					}
					setProjectsList(cloneDestination.alreadyClonedInto);
				}
				super.setVisible(visible);
			}
		};
	}

	@Override
	public boolean performCancel() {
		if (cloneDestination.alreadyClonedInto != null) {
			if (MessageDialog
					.openQuestion(getShell(), "Aborting clone.",
							"A complete clone was already made. Do you want to delete it?")) {
				deleteRecursively(new File(cloneDestination.alreadyClonedInto));
			}
		}
		return true;
	}

	private void deleteRecursively(File f) {
		for (File i : f.listFiles()) {
			if (i.isDirectory()) {
				deleteRecursively(i);
			} else {
				if (!i.delete()) {
					i.deleteOnExit();
				}
			}
		}
		if (!f.delete())
			f.deleteOnExit();
	}

	@Override
	public void addPages() {
		addPage(cloneSource);
		addPage(validSource);
		addPage(cloneDestination);
		addPage(importProject);
	}

	@Override
	public boolean canFinish() {
		return cloneDestination.isPageComplete()
				&& !cloneDestination.showImportWizard.getSelection()
				|| importProject.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		if (!cloneDestination.showImportWizard.getSelection())
			return performClone(true);
		return importProject.createProjects();
	}

	boolean performClone(boolean background) {
		final URIish uri = cloneSource.getSelection().getURI();
		final boolean allSelected = validSource.isAllSelected();
		final Collection<Ref> selectedBranches = validSource
				.getSelectedBranches();
		final File workdir = cloneDestination.getDestinationFile();
		final String branch = cloneDestination.getInitialBranch();
		final String remoteName = cloneDestination.getRemote();

		workdir.mkdirs();
		if (!workdir.isDirectory()) {
			final String errorMessage = NLS.bind(
					UIText.GitCloneWizard_errorCannotCreate, workdir.getPath());
			ErrorDialog.openError(getShell(), getWindowTitle(),
					UIText.GitCloneWizard_failed, new Status(IStatus.ERROR,
							Activator.getPluginId(), 0, errorMessage, null));
			// let's give user a chance to fix this minor problem
			return false;
		}

		final CloneOperation op = new CloneOperation(uri, allSelected,
				selectedBranches, workdir, branch, remoteName);
		if (background) {
			final Job job = new Job(NLS.bind(UIText.GitCloneWizard_jobName, uri
					.toString())) {
				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					try {
						op.run(monitor);
						return Status.OK_STATUS;
					} catch (InterruptedException e) {
						return Status.CANCEL_STATUS;
					} catch (InvocationTargetException e) {
						Throwable thr = e.getCause();
						return new Status(IStatus.ERROR, Activator
								.getPluginId(), 0, thr.getMessage(), thr);
					}
				}
			};
			job.setUser(true);
			job.schedule();
			return true;
		} else {
			try {
				PlatformUI.getWorkbench().getProgressService().run(false, true,
						op);
				return true;
			} catch (Exception e) {
				Activator.logError("Failed to clone", e);
				MessageDialog.openError(getShell(), "Failed clone", e
						.toString());
				return false;
			}
		}
	}
}
