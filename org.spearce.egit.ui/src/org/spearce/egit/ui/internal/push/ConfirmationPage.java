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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.spearce.egit.core.op.PushOperation;
import org.spearce.egit.core.op.PushOperationResult;
import org.spearce.egit.core.op.PushOperationSpecification;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.components.RefSpecPage;
import org.spearce.egit.ui.internal.components.RepositorySelection;
import org.spearce.egit.ui.internal.components.RepositorySelectionPage;
import org.spearce.egit.ui.internal.components.SelectionChangeListener;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.URIish;

class ConfirmationPage extends WizardPage {
	static Collection<RemoteRefUpdate> copyUpdates(
			final Collection<RemoteRefUpdate> refUpdates) throws IOException {
		final Collection<RemoteRefUpdate> copy = new ArrayList<RemoteRefUpdate>(
				refUpdates.size());
		for (final RemoteRefUpdate rru : refUpdates)
			copy.add(new RemoteRefUpdate(rru, null));
		return copy;
	}

	private final Repository local;

	private final RepositorySelectionPage repoPage;

	private final RefSpecPage refSpecPage;

	private RepositorySelection displayedRepoSelection;

	private List<RefSpec> displayedRefSpecs;

	private PushOperationResult confirmedResult;

	private PushResultTable resultPanel;

	private Button requireUnchangedButton;

	private Button showOnlyIfChanged;

	public ConfirmationPage(final Repository local,
			final RepositorySelectionPage repoPage,
			final RefSpecPage refSpecPage) {
		super(ConfirmationPage.class.getName());
		this.local = local;
		this.repoPage = repoPage;
		this.refSpecPage = refSpecPage;

		setTitle(UIText.ConfirmationPage_title);
		setDescription(UIText.ConfirmationPage_description);

		final SelectionChangeListener listener = new SelectionChangeListener() {
			public void selectionChanged() {
				checkPreviousPagesSelections();
			}
		};
		repoPage.addSelectionListener(listener);
		refSpecPage.addSelectionListener(listener);
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());

		resultPanel = new PushResultTable(panel);
		final Control tableControl = resultPanel.getControl();
		tableControl
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		requireUnchangedButton = new Button(panel, SWT.CHECK);
		requireUnchangedButton
				.setText(UIText.ConfirmationPage_requireUnchangedButton);

		showOnlyIfChanged = new Button(panel, SWT.CHECK);
		showOnlyIfChanged.setText(UIText.ConfirmationPage_showOnlyIfChanged);

		setControl(panel);
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible)
			revalidate();
		super.setVisible(visible);
	}

	boolean isConfirmed() {
		return confirmedResult != null;
	}

	PushOperationResult getConfirmedResult() {
		return confirmedResult;
	}

	boolean isRequireUnchangedSelected() {
		return requireUnchangedButton.getSelection();
	}

	boolean isShowOnlyIfChangedSelected() {
		return showOnlyIfChanged.getSelection();
	}

	private void checkPreviousPagesSelections() {
		if (!repoPage.selectionEquals(displayedRepoSelection)
				|| !refSpecPage.specsSelectionEquals(displayedRefSpecs)) {
			// Allow user to finish by skipping confirmation...
			setPageComplete(true);
		} else {
			// ... but if user doesn't skip confirmation, allow only when no
			// critical errors occurred
			setPageComplete(confirmedResult != null);
		}
	}

	private void revalidate() {
		// always update this page
		resultPanel.setData(local, null);
		confirmedResult = null;
		displayedRepoSelection = repoPage.getSelection();
		displayedRefSpecs = refSpecPage.getRefSpecs();
		setErrorMessage(null);
		setPageComplete(false);
		getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				revalidateImpl();
			}
		});
	}

	private void revalidateImpl() {
		if (getControl().isDisposed() || !isCurrentPage())
			return;

		final List<RefSpec> fetchSpecs;
		if (displayedRepoSelection.isConfigSelected())
			fetchSpecs = displayedRepoSelection.getConfig().getFetchRefSpecs();
		else
			fetchSpecs = null;

		final PushOperation operation;
		try {
			final Collection<RemoteRefUpdate> updates = Transport
					.findRemoteRefUpdatesFor(local, displayedRefSpecs,
							fetchSpecs);
			if (updates.isEmpty()) {
				// It can happen only when local refs changed in the mean time.
				setErrorMessage(UIText.ConfirmationPage_errorRefsChangedNoMatch);
				setPageComplete(false);
				return;
			}

			final PushOperationSpecification spec = new PushOperationSpecification();
			for (final URIish uri : displayedRepoSelection.getAllURIs())
				spec.addURIRefUpdates(uri, copyUpdates(updates));

			operation = new PushOperation(local, spec, true,
					displayedRepoSelection.getConfig());
			getContainer().run(true, true, operation);
		} catch (final IOException e) {
			setErrorMessage(NLS.bind(
					UIText.ConfirmationPage_errorCantResolveSpecs, e
							.getMessage()));
			return;
		} catch (final InvocationTargetException e) {
			setErrorMessage(NLS.bind(UIText.ConfirmationPage_errorUnexpected, e
					.getCause().getMessage()));
			return;
		} catch (final InterruptedException e) {
			setErrorMessage(UIText.ConfirmationPage_errorInterrupted);
			setPageComplete(true);
			displayedRefSpecs = null;
			displayedRepoSelection = null;
			return;
		}

		final PushOperationResult result = operation.getOperationResult();
		resultPanel.setData(local, result);
		if (result.isSuccessfulConnectionForAnyURI()) {
			setPageComplete(true);
			confirmedResult = result;
		} else {
			final String message = NLS.bind(
					UIText.ConfirmationPage_cantConnectToAny, result
							.getErrorStringForAllURis());
			setErrorMessage(message);
			ErrorDialog
					.openError(getShell(),
							UIText.ConfirmationPage_cantConnectToAnyTitle,
							null,
							new Status(IStatus.ERROR, Activator.getPluginId(),
									message));
		}
	}
}
