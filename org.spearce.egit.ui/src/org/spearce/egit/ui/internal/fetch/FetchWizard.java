/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.fetch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.spearce.egit.core.EclipseGitProgressTransformer;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.components.RefSpecPage;
import org.spearce.egit.ui.internal.components.RepositorySelection;
import org.spearce.egit.ui.internal.components.RepositorySelectionPage;
import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryConfig;
import org.spearce.jgit.transport.FetchResult;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.Transport;

/**
 * Wizard allowing user to specify all needed data to fetch from another
 * repository - including selection of remote repository, ref specifications,
 * annotated tags fetching strategy.
 * <p>
 * Fetch operation is performed upon successful completion of this wizard.
 */
public class FetchWizard extends Wizard {
	private final Repository localDb;

	private final RepositorySelectionPage repoPage;

	private final RefSpecPage refSpecPage;

	/**
	 * Create wizard for provided local repository.
	 *
	 * @param localDb
	 *            local repository to fetch to.
	 * @throws URISyntaxException
	 *             when configuration of this repository contains illegal URIs.
	 */
	public FetchWizard(final Repository localDb) throws URISyntaxException {
		this.localDb = localDb;
		final List<RemoteConfig> remotes = RemoteConfig
				.getAllRemoteConfigs(localDb.getConfig());
		repoPage = new RepositorySelectionPage(true, remotes);
		refSpecPage = new RefSpecPage(localDb, false, repoPage);
		// TODO use/create another cool icon
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(repoPage);
		addPage(refSpecPage);
	}

	@Override
	public boolean performFinish() {
		if (repoPage.getSelection().isConfigSelected()
				&& refSpecPage.isSaveRequested())
			saveConfig();

		final Transport transport;
		final RepositorySelection repoSelection = repoPage.getSelection();
		try {
			if (repoSelection.isConfigSelected())
				transport = Transport.open(localDb, repoSelection.getConfig());
			else
				transport = Transport.open(localDb, repoSelection.getURI());
		} catch (final NotSupportedException e) {
			ErrorDialog.openError(getShell(),
					UIText.FetchWizard_transportNotSupportedTitle,
					UIText.FetchWizard_transportNotSupportedMessage,
					new Status(IStatus.ERROR, org.spearce.egit.ui.Activator
							.getPluginId(), e.getMessage(), e));
			return false;
		}
		transport.setTagOpt(refSpecPage.getTagOpt());

		final Job fetchJob = new FetchJob(transport, refSpecPage.getRefSpecs(),
				getSourceString());
		fetchJob.setUser(true);
		fetchJob.schedule();
		return true;
	}

	@Override
	public String getWindowTitle() {
		final IWizardPage currentPage = getContainer().getCurrentPage();
		if (currentPage == repoPage || currentPage == null)
			return UIText.FetchWizard_windowTitleDefault;
		return NLS.bind(UIText.FetchWizard_windowTitleWithSource,
				getSourceString());
	}

	private void saveConfig() {
		final RemoteConfig rc = repoPage.getSelection().getConfig();
		rc.setFetchRefSpecs(refSpecPage.getRefSpecs());
		rc.setTagOpt(refSpecPage.getTagOpt());
		final RepositoryConfig config = localDb.getConfig();
		rc.update(config);
		try {
			config.save();
		} catch (final IOException e) {
			ErrorDialog.openError(getShell(), UIText.FetchWizard_cantSaveTitle,
					UIText.FetchWizard_cantSaveMessage, new Status(
							IStatus.WARNING, Activator.getPluginId(), e
									.getMessage(), e));
			// Continue, it's not critical.
		}
	}

	private String getSourceString() {
		final RepositorySelection repoSelection = repoPage.getSelection();
		if (repoSelection.isConfigSelected())
			return repoSelection.getConfigName();
		return repoSelection.getURI().toString();
	}

	private class FetchJob extends Job {
		private final Transport transport;

		private final List<RefSpec> refSpecs;

		private final String sourceString;

		public FetchJob(final Transport transport,
				final List<RefSpec> refSpecs, final String sourceString) {
			super(NLS.bind(UIText.FetchWizard_jobName, sourceString));
			this.transport = transport;
			this.refSpecs = refSpecs;
			this.sourceString = sourceString;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor == null)
				monitor = new NullProgressMonitor();
			final FetchResult result;
			try {
				result = transport.fetch(new EclipseGitProgressTransformer(
						monitor), refSpecs);
			} catch (final NotSupportedException e) {
				return new Status(IStatus.ERROR, Activator.getPluginId(),
						UIText.FetchWizard_fetchNotSupported, e);
			} catch (final TransportException e) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				return new Status(IStatus.ERROR, Activator.getPluginId(),
						UIText.FetchWizard_transportError, e);
			}

			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					final Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					final Dialog dialog = new FetchResultDialog(shell, localDb,
							result, sourceString);
					dialog.open();
				}
			});
			return Status.OK_STATUS;
		}
	}
}
