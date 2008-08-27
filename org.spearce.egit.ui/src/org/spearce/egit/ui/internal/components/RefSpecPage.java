/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.spearce.egit.core.op.ListRemoteOperation;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.TagOpt;
import org.spearce.jgit.transport.URIish;

/**
 * This wizard page allows user easy selection of specifications for push or
 * fetch (configurable).
 * <p>
 * Page is relying highly on {@link RefSpecPanel} component, see its description
 * for details.
 * <p>
 * Page is designed to be successor of {@link RepositorySelectionPage} in
 * wizard.
 */
public class RefSpecPage extends BaseWizardPage {

	private final Repository local;

	private final RepositorySelectionPage repoPage;

	private final boolean pushPage;

	private RepositorySelection validatedRepoSelection;

	private RefSpecPanel specsPanel;

	private Button saveButton;

	private Button tagsAutoFollowButton;

	private Button tagsFetchTagsButton;

	private Button tagsNoTagsButton;

	private String transportError;

	/**
	 * Create specifications selection page for provided context.
	 *
	 * @param local
	 *            local repository.
	 * @param pushPage
	 *            true if this page is used for push specifications selection,
	 *            false if it used for fetch specifications selection.
	 * @param repoPage
	 *            repository selection page - must be predecessor of this page
	 *            in wizard.
	 */
	public RefSpecPage(final Repository local, final boolean pushPage,
			final RepositorySelectionPage repoPage) {
		super(RefSpecPage.class.getName());
		this.local = local;
		this.repoPage = repoPage;
		this.pushPage = pushPage;
		if (pushPage) {
			setTitle(UIText.RefSpecPage_titlePush);
			setDescription(UIText.RefSpecPage_descriptionPush);
		} else {
			setTitle(UIText.RefSpecPage_titleFetch);
			setDescription(UIText.RefSpecPage_descriptionFetch);
		}

		repoPage.addSelectionListener(new SelectionChangeListener() {
			public void selectionChanged() {
				if (!repoPage.selectionEquals(validatedRepoSelection))
					setPageComplete(false);
				else
					checkPage();
			}
		});
	}

	public void createControl(Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		panel.setLayout(new GridLayout());

		specsPanel = new RefSpecPanel(panel, pushPage);
		specsPanel.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		specsPanel.addRefSpecTableListener(new SelectionChangeListener() {
			public void selectionChanged() {
				notifySelectionChanged();
				checkPage();
			}
		});

		final SelectionAdapter changesNotifier = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				notifySelectionChanged();
			}
		};
		if (!pushPage) {
			final Group tagsGroup = new Group(panel, SWT.NULL);
			tagsGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					false));
			tagsGroup.setText(UIText.RefSpecPage_annotatedTagsGroup);
			tagsGroup.setLayout(new GridLayout());
			tagsAutoFollowButton = new Button(tagsGroup, SWT.RADIO);
			tagsAutoFollowButton
					.setText(UIText.RefSpecPage_annotatedTagsAutoFollow);
			tagsFetchTagsButton = new Button(tagsGroup, SWT.RADIO);
			tagsFetchTagsButton
					.setText(UIText.RefSpecPage_annotatedTagsFetchTags);
			tagsNoTagsButton = new Button(tagsGroup, SWT.RADIO);
			tagsNoTagsButton
					.setText(UIText.RefSpecPage_annotatedTagsNoTags);
			tagsAutoFollowButton.addSelectionListener(changesNotifier);
			tagsFetchTagsButton.addSelectionListener(changesNotifier);
			tagsNoTagsButton.addSelectionListener(changesNotifier);
		}

		saveButton = new Button(panel, SWT.CHECK);
		saveButton.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		saveButton.addSelectionListener(changesNotifier);

		setControl(panel);
		notifySelectionChanged();
		checkPage();
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible)
			revalidate();
		super.setVisible(visible);
	}

	/**
	 * @return ref specifications as selected by user. Returned collection is a
	 *         copy, so it may be modified by caller.
	 */
	public List<RefSpec> getRefSpecs() {
		if (specsPanel == null)
			return Collections.emptyList();
		else
			return new ArrayList<RefSpec>(specsPanel.getRefSpecs());
	}

	/**
	 * @return true if user chosen to save selected specification in remote
	 *         configuration, false otherwise.
	 */
	public boolean isSaveRequested() {
		return saveButton.getSelection();
	}

	/**
	 * @return selected tag fetching strategy. This result is relevant only for
	 *         fetch page.
	 */
	public TagOpt getTagOpt() {
		if (tagsAutoFollowButton.getSelection())
			return TagOpt.AUTO_FOLLOW;
		if (tagsFetchTagsButton.getSelection())
			return TagOpt.FETCH_TAGS;
		return TagOpt.NO_TAGS;
	}

	/**
	 * Compare provided specifications to currently selected ones.
	 *
	 * @param specs
	 *            specifications to compare to. May be null.
	 * @return true if provided specifications are equal to currently selected
	 *         ones, false otherwise.
	 */
	public boolean specsSelectionEquals(final List<RefSpec> specs) {
		return getRefSpecs().equals(specs);
	}

	private void revalidate() {
		final RepositorySelection newRepoSelection = repoPage.getSelection();

		if (repoPage.selectionEquals(validatedRepoSelection)) {
			// nothing changed on previous page
			checkPage();
			return;
		}

		specsPanel.clearRefSpecs();
		specsPanel.setEnable(false);
		saveButton.setVisible(false);
		saveButton.setSelection(false);
		notifySelectionChanged();
		validatedRepoSelection = null;
		transportError = null;
		getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				revalidateImpl(newRepoSelection);
			}
		});
	}

	private void revalidateImpl(final RepositorySelection newRepoSelection) {
		final ListRemoteOperation listRemotesOp;
		try {
			final URIish uri;
			uri = newRepoSelection.getURI();
			listRemotesOp = new ListRemoteOperation(local, uri);
			getContainer().run(true, true, listRemotesOp);
		} catch (InvocationTargetException e) {
			final Throwable cause = e.getCause();
			transportError(cause.getMessage());
			ErrorDialog.openError(getShell(),
					UIText.RefSpecPage_errorTransportDialogTitle,
					UIText.RefSpecPage_errorTransportDialogMessage, new Status(
							IStatus.ERROR, Activator.getPluginId(), 0, cause
									.getMessage(), cause));
			return;
		} catch (InterruptedException e) {
			transportError(UIText.RefSpecPage_operationCancelled);
			return;
		}

		this.validatedRepoSelection = newRepoSelection;
		final String remoteName = validatedRepoSelection.getConfigName();
		specsPanel.setAssistanceData(local, listRemotesOp.getRemoteRefs(),
				remoteName);

		tagsAutoFollowButton.setSelection(false);
		tagsFetchTagsButton.setSelection(false);
		tagsNoTagsButton.setSelection(false);

		if (newRepoSelection.isConfigSelected()) {
			saveButton.setVisible(true);
			saveButton.setText(NLS.bind(UIText.RefSpecPage_saveSpecifications,
					remoteName));
			saveButton.getParent().layout();
			final TagOpt tagOpt = newRepoSelection.getConfig().getTagOpt();
			switch (tagOpt) {
			case AUTO_FOLLOW:
				tagsAutoFollowButton.setSelection(true);
				break;
			case FETCH_TAGS:
				tagsFetchTagsButton.setSelection(true);
				break;
			case NO_TAGS:
				tagsNoTagsButton.setSelection(true);
				break;
			}
		} else
			tagsAutoFollowButton.setSelection(true);

		checkPage();
	}

	private void transportError(final String message) {
		transportError = message;
		checkPage();
	}

	private void checkPage() {
		if (transportError != null) {
			setErrorMessage(transportError);
			setPageComplete(false);
			return;
		}
		if (!specsPanel.isEmpty() && specsPanel.isValid()
				&& !specsPanel.isMatchingAnyRefs()) {
			setErrorMessage(UIText.RefSpecPage_errorDontMatchSrc);
			setPageComplete(false);
			return;
		}
		setErrorMessage(specsPanel.getErrorMessage());
		setPageComplete(!specsPanel.isEmpty() && specsPanel.isValid());
	}
}
