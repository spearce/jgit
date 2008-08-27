/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.push;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.spearce.egit.core.op.PushOperation;
import org.spearce.egit.core.op.PushOperationResult;
import org.spearce.egit.core.op.PushOperationSpecification;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.components.RefSpecPage;
import org.spearce.egit.ui.internal.components.RepositorySelection;
import org.spearce.egit.ui.internal.components.RepositorySelectionPage;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryConfig;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.URIish;

/**
 * Wizard allowing user to specify all needed data to push to another repository
 * - including selection of remote repository and refs specifications.
 * <p>
 * Push operation is performed upon successful completion of this wizard.
 */
public class PushWizard extends Wizard {
	private static String getURIsString(final Collection<URIish> uris) {
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final URIish uri : uris) {
			if (first)
				first = false;
			else
				sb.append(", "); //$NON-NLS-1$
			sb.append(uri);
		}
		return sb.toString();
	}

	private Repository localDb;

	private final RepositorySelectionPage repoPage;

	private final RefSpecPage refSpecPage;

	private ConfirmationPage confirmPage;

	/**
	 * Create push wizard for specified local repository.
	 *
	 * @param localDb
	 *            repository to push from.
	 * @throws URISyntaxException
	 *             when configuration of this repository contains illegal URIs.
	 */
	public PushWizard(final Repository localDb) throws URISyntaxException {
		this.localDb = localDb;
		final List<RemoteConfig> remotes = RemoteConfig
				.getAllRemoteConfigs(localDb.getConfig());
		repoPage = new RepositorySelectionPage(false, remotes);
		refSpecPage = new RefSpecPage(localDb, true, repoPage);
		confirmPage = new ConfirmationPage(localDb, repoPage, refSpecPage);
		// TODO use/create another cool icon
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(repoPage);
		addPage(refSpecPage);
		addPage(confirmPage);
	}

	@Override
	public boolean performFinish() {
		if (repoPage.getSelection().isConfigSelected()
				&& refSpecPage.isSaveRequested()) {
			saveRefSpecs();
		}

		final PushOperation operation = createPushOperation();
		if (operation == null)
			return false;
		final PushOperationResult resultToCompare;
		if (confirmPage.isShowOnlyIfChangedSelected())
			resultToCompare = confirmPage.getConfirmedResult();
		else
			resultToCompare = null;
		final Job job = new PushJob(operation, resultToCompare,
				getDestinationString());

		job.setUser(true);
		job.schedule();

		return true;
	}

	@Override
	public String getWindowTitle() {
		final IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage == repoPage || currentPage == null)
			return UIText.PushWizard_windowTitleDefault;
		final String destination = getDestinationString();
		return NLS.bind(UIText.PushWizard_windowTitleWithDestination,
				destination);
	}

	private void saveRefSpecs() {
		final RemoteConfig rc = repoPage.getSelection().getConfig();
		rc.setPushRefSpecs(refSpecPage.getRefSpecs());
		final RepositoryConfig config = localDb.getConfig();
		rc.update(config);
		try {
			config.save();
		} catch (final IOException e) {
			ErrorDialog.openError(getShell(), UIText.PushWizard_cantSaveTitle,
					UIText.PushWizard_cantSaveMessage, new Status(
							IStatus.WARNING, Activator.getPluginId(), e
									.getMessage(), e));
			// Continue, it's not critical.
		}
	}

	private PushOperation createPushOperation() {
		try {
			final PushOperationSpecification spec;
			final RemoteConfig config = repoPage.getSelection().getConfig();
			if (confirmPage.isConfirmed()) {
				final PushOperationResult confirmedResult = confirmPage
						.getConfirmedResult();
				spec = confirmedResult.deriveSpecification(confirmPage
						.isRequireUnchangedSelected());
			} else {
				final Collection<RefSpec> fetchSpecs;
				if (config != null)
					fetchSpecs = config.getFetchRefSpecs();
				else
					fetchSpecs = null;

				final Collection<RemoteRefUpdate> updates = Transport
						.findRemoteRefUpdatesFor(localDb, refSpecPage
								.getRefSpecs(), fetchSpecs);
				if (updates.isEmpty()) {
					ErrorDialog.openError(getShell(),
							UIText.PushWizard_missingRefsTitle, null,
							new Status(IStatus.ERROR, Activator.getPluginId(),
									UIText.PushWizard_missingRefsMessage));
					return null;
				}

				spec = new PushOperationSpecification();
				for (final URIish uri : repoPage.getSelection().getAllURIs())
					spec.addURIRefUpdates(uri, ConfirmationPage
							.copyUpdates(updates));
			}
			return new PushOperation(localDb, spec, false, config);
		} catch (final IOException e) {
			ErrorDialog.openError(getShell(),
					UIText.PushWizard_cantPrepareUpdatesTitle,
					UIText.PushWizard_cantPrepareUpdatesMessage, new Status(
							IStatus.ERROR, Activator.getPluginId(), e
									.getMessage(), e));
			return null;
		}
	}

	private String getDestinationString() {
		final RepositorySelection repoSelection = repoPage.getSelection();
		final String destination;
		if (repoSelection.isConfigSelected())
			destination = repoSelection.getConfigName();
		else
			destination = repoSelection.getURI().toString();
		return destination;
	}

	private class PushJob extends Job {
		private final PushOperation operation;

		private final PushOperationResult resultToCompare;

		private final String destinationString;

		public PushJob(final PushOperation operation,
				final PushOperationResult resultToCompare,
				final String destinationString) {
			super(NLS.bind(UIText.PushWizard_jobName, getURIsString(operation
					.getSpecification().getURIs())));
			this.operation = operation;
			this.resultToCompare = resultToCompare;
			this.destinationString = destinationString;
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			try {
				operation.run(monitor);
			} catch (final InvocationTargetException e) {
				return new Status(IStatus.ERROR, Activator.getPluginId(),
						UIText.PushWizard_unexpectedError, e.getCause());
			}

			final PushOperationResult result = operation.getOperationResult();
			if (!result.isSuccessfulConnectionForAnyURI()) {
				return new Status(IStatus.ERROR, Activator.getPluginId(), NLS
						.bind(UIText.PushWizard_cantConnectToAny, result
								.getErrorStringForAllURis()));
			}

			if (resultToCompare == null || !result.equals(resultToCompare)) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(
						new Runnable() {
							public void run() {
								final Shell shell = PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow().getShell();
								final Dialog dialog = new PushResultDialog(
										shell, localDb, result,
										destinationString);
								dialog.open();
							}
						});
			}
			return Status.OK_STATUS;
		}
	}
}
