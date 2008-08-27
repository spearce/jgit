/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.push;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.spearce.egit.core.op.PushOperationResult;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.Repository;

class PushResultDialog extends Dialog {
	private final Repository localDb;

	private final PushOperationResult result;

	private final String destinationString;

	PushResultDialog(final Shell parentShell, final Repository localDb,
			final PushOperationResult result, final String destinationString) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.localDb = localDb;
		this.result = result;
		this.destinationString = destinationString;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);

		final Label label = new Label(composite, SWT.NONE);
		label.setText(NLS.bind(UIText.ResultDialog_label, destinationString));
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		final PushResultTable table = new PushResultTable(composite);
		table.setData(localDb, result);
		final Control tableControl = table.getControl();
		final GridData tableLayout = new GridData(SWT.FILL, SWT.FILL, true,
				true);
		tableLayout.widthHint = 650;
		tableLayout.heightHint = 300;
		tableControl.setLayoutData(tableLayout);

		getShell().setText(
				NLS.bind(UIText.ResultDialog_title, destinationString));
		return composite;
	}
}
