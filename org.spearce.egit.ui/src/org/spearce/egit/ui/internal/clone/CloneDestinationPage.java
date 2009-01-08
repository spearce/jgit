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
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.components.RepositorySelection;
import org.spearce.egit.ui.internal.components.RepositorySelectionPage;
import org.spearce.egit.ui.internal.components.SelectionChangeListener;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.Ref;

/**
 * Wizard page that allows the user entering the location of a repository to be
 * cloned.
 */
class CloneDestinationPage extends WizardPage {
	private final RepositorySelectionPage sourcePage;

	private final SourceBranchPage branchPage;

	private RepositorySelection validatedRepoSelection;

	private List<Ref> validatedSelectedBranches;

	private Ref validatedHEAD;

	private Combo initialBranch;

	private Text directoryText;

	private Text remoteText;

	Button showImportWizard;

	String alreadyClonedInto;

	CloneDestinationPage(final RepositorySelectionPage sp,
			final SourceBranchPage bp) {
		super(CloneDestinationPage.class.getName());
		sourcePage = sp;
		branchPage = bp;
		setTitle(UIText.CloneDestinationPage_title);

		final SelectionChangeListener listener = new SelectionChangeListener() {
			public void selectionChanged() {
				checkPreviousPagesSelections();
			}
		};
		sourcePage.addSelectionListener(listener);
		branchPage.addSelectionListener(listener);
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		createDestinationGroup(panel);
		createConfigGroup(panel);
		createWorkbenchGroup(panel);
		setControl(panel);
		checkPage();
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible)
			revalidate();
		super.setVisible(visible);
		if (visible)
			directoryText.setFocus();
	}

	private void checkPreviousPagesSelections() {
		if (!sourcePage.selectionEquals(validatedRepoSelection)
				|| !branchPage.selectionEquals(validatedSelectedBranches,
						validatedHEAD))
			setPageComplete(false);
		else
			checkPage();
	}

	private void createDestinationGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.CloneDestinationPage_groupDestination);

		newLabel(g, UIText.CloneDestinationPage_promptDirectory + ":");
		final Composite p = new Composite(g, SWT.NONE);
		final GridLayout grid = new GridLayout();
		grid.numColumns = 2;
		p.setLayout(grid);
		p.setLayoutData(createFieldGridData());
		directoryText = new Text(p, SWT.BORDER);
		directoryText.setLayoutData(createFieldGridData());
		directoryText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				checkPage();
			}
		});
		final Button b = new Button(p, SWT.PUSH);
		b.setText(UIText.CloneDestinationPage_browseButton);
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				final FileDialog d;

				d = new FileDialog(getShell(), SWT.APPLICATION_MODAL | SWT.SAVE);
				if (directoryText.getText().length() > 0) {
					final File file = new File(directoryText.getText())
							.getAbsoluteFile();
					d.setFilterPath(file.getParent());
					d.setFileName(file.getName());
				}
				final String r = d.open();
				if (r != null)
					directoryText.setText(r);
			}
		});

		newLabel(g, UIText.CloneDestinationPage_promptInitialBranch + ":");
		initialBranch = new Combo(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		initialBranch.setLayoutData(createFieldGridData());
		initialBranch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				checkPage();
			}
		});
	}

	private void createConfigGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.CloneDestinationPage_groupConfiguration);

		newLabel(g, UIText.CloneDestinationPage_promptRemoteName + ":");
		remoteText = new Text(g, SWT.BORDER);
		remoteText.setText("origin");
		remoteText.setLayoutData(createFieldGridData());
		remoteText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
	}

	private void createWorkbenchGroup(Composite parent) {
		final Group g = createGroup(parent, UIText.CloneDestinationPage_workspaceImport);
		showImportWizard = new Button(g, SWT.CHECK);
		showImportWizard.setSelection(true);
		showImportWizard.setText(UIText.CloneDestinationPage_importProjectsAfterClone);
		showImportWizard.setLayoutData(createFieldGridData());
		showImportWizard.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});
	}

	private static Group createGroup(final Composite parent, final String text) {
		final Group g = new Group(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		g.setLayout(layout);
		g.setText(text);
		final GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		g.setLayoutData(gd);
		return g;
	}

	private static void newLabel(final Group g, final String text) {
		new Label(g, SWT.NULL).setText(text);
	}

	private static GridData createFieldGridData() {
		return new GridData(SWT.FILL, SWT.DEFAULT, true, false);
	}

	/**
	 * @return location the user wants to store this repository.
	 */
	public File getDestinationFile() {
		return new File(directoryText.getText());
	}

	/**
	 * @return initial branch selected (includes refs/heads prefix).
	 */
	public String getInitialBranch() {
		final int ix = initialBranch.getSelectionIndex();
		if (ix < 0)
			return Constants.R_HEADS + Constants.MASTER;
		return Constants.R_HEADS + initialBranch.getItem(ix);
	}

	/**
	 * @return remote name
	 */
	public String getRemote() {
		return remoteText.getText();
	}

	/**
	 * Check internal state for page completion status.
	 */
	private void checkPage() {
		final String dstpath = directoryText.getText();
		if (dstpath.length() == 0) {
			setErrorMessage(NLS.bind(UIText.CloneDestinationPage_fieldRequired,
					UIText.CloneDestinationPage_promptDirectory));
			setPageComplete(false);
			return;
		}
		final File absoluteFile = new File(dstpath).getAbsoluteFile();
		if (!absoluteFile.getAbsolutePath().equals(alreadyClonedInto)
				&& !isEmptyDir(absoluteFile)) {
			setErrorMessage(NLS.bind(
					UIText.CloneDestinationPage_errorNotEmptyDir, absoluteFile
							.getPath()));
			setPageComplete(false);
			return;
		}

		if (!canCreateSubdir(absoluteFile.getParentFile())) {
			setErrorMessage(NLS.bind(UIText.GitCloneWizard_errorCannotCreate,
					absoluteFile.getPath()));
			setPageComplete(false);
			return;
		}
		if (initialBranch.getSelectionIndex() < 0) {
			setErrorMessage(NLS.bind(UIText.CloneDestinationPage_fieldRequired,
					UIText.CloneDestinationPage_promptInitialBranch));
			setPageComplete(false);
			return;
		}
		if (remoteText.getText().length() == 0) {
			setErrorMessage(NLS.bind(UIText.CloneDestinationPage_fieldRequired,
					UIText.CloneDestinationPage_promptRemoteName));
			setPageComplete(false);
			return;
		}

		setErrorMessage(null);
		setPageComplete(true);
	}

	private static boolean isEmptyDir(final File dir) {
		if (!dir.exists())
			return true;
		if (!dir.isDirectory())
			return false;
		return dir.listFiles().length == 0;
	}

	// this is actually just an optimistic heuristic - should be named
	// isThereHopeThatCanCreateSubdir() as probably there is no 100% reliable
	// way to check that in Java for Windows
	private static boolean canCreateSubdir(final File parent) {
		if (parent == null)
			return true;
		if (parent.exists())
			return parent.isDirectory() && parent.canWrite();
		return canCreateSubdir(parent.getParentFile());
	}

	private void revalidate() {
		if (sourcePage.selectionEquals(validatedRepoSelection)
				&& branchPage.selectionEquals(validatedSelectedBranches,
						validatedHEAD)) {
			checkPage();
			return;
		}

		if (!sourcePage.selectionEquals(validatedRepoSelection)) {
			validatedRepoSelection = sourcePage.getSelection();
			// update repo-related selection only if it changed
			final String n = getSuggestedName();
			setDescription(NLS.bind(UIText.CloneDestinationPage_description, n));
			directoryText.setText(new File(ResourcesPlugin.getWorkspace()
					.getRoot().getRawLocation().toFile(), n).getAbsolutePath());
		}

		validatedSelectedBranches = branchPage.getSelectedBranches();
		validatedHEAD = branchPage.getHEAD();

		initialBranch.removeAll();
		final Ref head = branchPage.getHEAD();
		int newix = 0;
		for (final Ref r : branchPage.getSelectedBranches()) {
			String name = r.getName();
			if (name.startsWith(Constants.R_HEADS))
				name = name.substring((Constants.R_HEADS).length());
			if (head != null && head.getName().equals(r.getName()))
				newix = initialBranch.getItemCount();
			initialBranch.add(name);
		}
		initialBranch.select(newix);
		checkPage();
	}

	private String getSuggestedName() {
		String path = validatedRepoSelection.getURI().getPath();
		int s = path.lastIndexOf('/');
		if (s != -1)
			path = path.substring(s + 1);
		if (path.endsWith(".git"))
			path = path.substring(0, path.length() - 4);
		return path;
	}

	@Override
	public boolean canFlipToNextPage() {
		return super.canFlipToNextPage() && showImportWizard.getSelection();
	}
}