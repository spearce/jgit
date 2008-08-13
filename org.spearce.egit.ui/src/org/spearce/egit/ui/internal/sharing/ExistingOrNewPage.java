/*******************************************************************************
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.sharing;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;

class ExistingOrNewPage extends WizardPage {
	final SharingWizard myWizard;
	private Button createInParent;

	ExistingOrNewPage(final SharingWizard w) {
		super(ExistingOrNewPage.class.getName());
		setTitle(UIText.ExistingOrNewPage_title);
		setDescription(UIText.ExistingOrNewPage_description);
		setImageDescriptor(UIIcons.WIZBAN_CONNECT_REPO);
		myWizard = w;
	}

	public void createControl(final Composite parent) {
		final Group g;
		final Button useExisting;
		final Button createNew;

		g = new Group(parent, SWT.NONE);
		g.setText(UIText.ExistingOrNewPage_groupHeader);
		g.setLayout(new RowLayout(SWT.VERTICAL));

		useExisting = new Button(g, SWT.RADIO);
		useExisting.setText(UIText.ExistingOrNewPage_useExisting);
		useExisting.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(final SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(final SelectionEvent e) {
				myWizard.setUseExisting();
				createInParent.setEnabled(false);
			}
		});
		useExisting.setSelection(true);

		createNew = new Button(g, SWT.RADIO);
		createNew.setEnabled(myWizard.canCreateNew());
		createNew.setText(UIText.ExistingOrNewPage_createNew);
		createNew.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(final SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(final SelectionEvent e) {
				myWizard.setCreateNew();
				createInParent.setEnabled(true);
			}
		});

		createInParent = new Button(g, SWT.CHECK);
		createInParent.setEnabled(createNew.getSelection());
		createInParent.setText(UIText.ExistingOrNewPage_createInParent);
		createInParent.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				myWizard.setUseParent(createInParent.getSelection());
			}
		});
		createInParent.setSelection(true);
		myWizard.setUseParent(createInParent.getSelection());
		setControl(g);
	}
}
