/*
 *  Copyright (C) 2008  Roger C. Soares
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
package org.spearce.egit.ui.internal.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.spearce.egit.ui.UIIcons;

/**
 * Import Git Repository Wizard. A front end to a git clone operation.
 */
public class GitCloneWizard extends Wizard implements IImportWizard {
	private WarningPage warning;

	public void init(IWorkbench arg0, IStructuredSelection arg1) {
		// Empty
	}

	@Override
	public void addPages() {
		warning = new WarningPage();
		addPage(warning);
	}

	@Override
	public boolean performFinish() {
		return true;
	}
}

class WarningPage extends WizardPage {
	private Composite container;

	/**
	 * Warning message for new users alerting on how to use egit.
	 */
	public WarningPage() {
		super("Warning Page", "Warning", UIIcons.WIZBAN_IMPORT_REPO);
		setDescription("Git Import is not ready yet.");
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		FillLayout layout = new FillLayout();
		container.setLayout(layout);
		Label label1 = new Label(container, SWT.NULL);
		label1.setText("\nUse Git (THE Git) to create or clone your repo."
				+ "\nSelect the project in the navigator and"
				+ "\ngo to the context menu's Team item and"
				+ "\nbelow it you will find \"Share Project\"."
				+ "\nSelect it and the rest is self explanatory.");

		setControl(container);
		setPageComplete(false);
	}

	@Override
	public Control getControl() {
		return container;
	}
}
