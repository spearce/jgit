/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.clone;

import java.io.File;
import java.net.URISyntaxException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.transport.URIish;

/**
 * Wizard page that allows the user entering the location of a repository to be
 * cloned.
 */
class CloneDestinationPage extends WizardPage {
	private final CloneSourcePage sourcePage;

	private final SourceBranchPage branchPage;

	private URIish validated;

	private Combo initialBranch;

	private Text directoryText;

	private Text remoteText;

	CloneDestinationPage(final CloneSourcePage sp, final SourceBranchPage bp) {
		super(CloneDestinationPage.class.getName());
		sourcePage = sp;
		branchPage = bp;

		setTitle(UIText.CloneDestinationPage_title);
		setImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);

		sourcePage.addURIishChangeListener(new URIishChangeListener() {
			public void uriishChanged(final URIish newURI) {
				if (newURI == null || !newURI.equals(validated))
					setPageComplete(false);
			}
		});
		branchPage.addBranchChangeListener(new BranchChangeListener() {
			public void branchesChanged() {
				setPageComplete(false);
			}
		});
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		createDestinationGroup(panel);
		createConfigGroup(panel);

		setControl(panel);
		setPageComplete(isPageComplete());
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible)
			revalidate();
		super.setVisible(visible);
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
				setPageComplete(isPageComplete());
			}
		});
		final Button b = new Button(p, SWT.PUSH);
		b.setText(UIText.CloneDestinationPage_browseButton);
		b.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing.
			}

			public void widgetSelected(final SelectionEvent e) {
				final FileDialog d;

				d = new FileDialog(getShell(), SWT.APPLICATION_MODAL | SWT.SAVE);
				if (directoryText.getText().length() > 0) {
					final File f = new File(directoryText.getText());
					d.setFilterPath(f.getAbsoluteFile().getAbsolutePath());
					d.setFileName(f.getName());
				}
				final String r = d.open();
				if (r != null)
					directoryText.setText(r);
			}
		});

		newLabel(g, UIText.CloneDestinationPage_promptInitialBranch + ":");
		initialBranch = new Combo(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		initialBranch.setLayoutData(createFieldGridData());
	}

	private void createConfigGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.CloneDestinationPage_groupConfiguration);

		newLabel(g, UIText.CloneDestinationPage_promptRemoteName + ":");
		remoteText = new Text(g, SWT.BORDER);
		remoteText.setText("origin");
		remoteText.setLayoutData(createFieldGridData());
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
			return Constants.HEADS_PREFIX + "/" + Constants.MASTER;
		return Constants.HEADS_PREFIX + "/" + initialBranch.getItem(ix);
	}

	/**
	 * @return remote name
	 */
	public String getRemote() {
		return remoteText.getText();
	}

	@Override
	public boolean isPageComplete() {
		final String dstpath = directoryText.getText();
		if (dstpath.length() == 0) {
			setErrorMessage(NLS.bind(UIText.CloneDestinationPage_fieldRequired,
					UIText.CloneDestinationPage_promptDirectory));
			return false;
		}
		if (new File(dstpath).exists()) {
			setErrorMessage(NLS.bind(UIText.CloneDestinationPage_errorExists,
					new File(dstpath).getName()));
			return false;
		}
		if (initialBranch.getSelectionIndex() < 0) {
			setErrorMessage(NLS.bind(UIText.CloneDestinationPage_fieldRequired,
					UIText.CloneDestinationPage_promptInitialBranch));
			return false;
		}
		if (remoteText.getText().length() == 0) {
			setErrorMessage(NLS.bind(UIText.CloneDestinationPage_fieldRequired,
					UIText.CloneDestinationPage_promptRemoteName));
			return false;
		}

		setErrorMessage(null);
		return true;
	}

	private void revalidate() {
		URIish newURI = null;
		try {
			newURI = sourcePage.getURI();
			validated = newURI;
		} catch (URISyntaxException e) {
			validated = null;
		}

		if (newURI == null || !newURI.equals(validated)) {
			final String n = getSuggestedName();
			setDescription(NLS.bind(UIText.CloneDestinationPage_description,
					n != null ? n : "<unknown>"));

			if (n != null) {
				directoryText.setText(new File(ResourcesPlugin.getWorkspace()
						.getRoot().getRawLocation().toFile(), n)
						.getAbsolutePath());
			}
		}

		initialBranch.removeAll();
		final Ref head = branchPage.getHEAD();
		int newix = 0;
		for (final Ref r : branchPage.getSelectedBranches()) {
			String name = r.getName();
			if (name.startsWith(Constants.HEADS_PREFIX + "/"))
				name = name.substring((Constants.HEADS_PREFIX + "/").length());
			if (head != null && head.getName().equals(r.getName()))
				newix = initialBranch.getItemCount();
			initialBranch.add(name);
		}
		initialBranch.select(newix);
	}

	private String getSuggestedName() {
		if (validated == null)
			return null;

		String path = validated.getPath();
		int s = path.lastIndexOf('/');
		if (s != -1)
			path = path.substring(s + 1);
		if (path.endsWith(".git"))
			path = path.substring(0, path.length() - 4);
		return path;
	}

}