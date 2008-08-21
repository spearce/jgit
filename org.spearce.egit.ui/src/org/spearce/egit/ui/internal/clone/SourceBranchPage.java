/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.spearce.egit.core.op.ListRemoteOperation;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.components.BaseWizardPage;
import org.spearce.egit.ui.internal.components.RepositorySelection;
import org.spearce.egit.ui.internal.components.RepositorySelectionPage;
import org.spearce.egit.ui.internal.components.SelectionChangeListener;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.transport.URIish;

class SourceBranchPage extends BaseWizardPage {
	private final RepositorySelectionPage sourcePage;

	private RepositorySelection validatedRepoSelection;

	private Ref head;

	private List<Ref> availableRefs = new ArrayList<Ref>();

	private List<Ref> selectedRefs = new ArrayList<Ref>();

	private Label label;

	private Table refsTable;

	private String transportError;

	SourceBranchPage(final RepositorySelectionPage sp) {
		super(SourceBranchPage.class.getName());
		sourcePage = sp;
		setTitle(UIText.SourceBranchPage_title);
		setDescription(UIText.SourceBranchPage_description);

		sourcePage.addSelectionListener(new SelectionChangeListener() {
			public void selectionChanged() {
				if (!sourcePage.selectionEquals(validatedRepoSelection))
					setPageComplete(false);
				else
					checkPage();
			}
		});
	}

	List<Ref> getSelectedBranches() {
		return new ArrayList<Ref>(selectedRefs);
	}

	Ref getHEAD() {
		return head;
	}

	boolean isAllSelected() {
		return availableRefs.size() == selectedRefs.size();
	}

	boolean selectionEquals(final List<Ref> selectedRefs, final Ref head) {
		return this.selectedRefs.equals(selectedRefs) && this.head == head;
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		label = new Label(panel, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		refsTable = new Table(panel, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER);
		refsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		refsTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (e.detail != SWT.CHECK)
					return;

				final TableItem tableItem = (TableItem) e.item;
				final int i = refsTable.indexOf(tableItem);
				final Ref ref = availableRefs.get(i);

				if (tableItem.getChecked()) {
					int insertionPos = 0;
					for (int j = 0; j < i; j++) {
						if (selectedRefs.contains(availableRefs.get(j)))
							insertionPos++;
					}
					selectedRefs.add(insertionPos, ref);
				} else
					selectedRefs.remove(ref);

				notifySelectionChanged();
				checkPage();
			}
		});

		final Composite bPanel = new Composite(panel, SWT.NONE);
		bPanel.setLayout(new RowLayout());
		final Button selectB;
		selectB = new Button(bPanel, SWT.PUSH);
		selectB.setText(UIText.SourceBranchPage_selectAll);
		selectB.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				for (int i = 0; i < refsTable.getItemCount(); i++)
					refsTable.getItem(i).setChecked(true);
				selectedRefs.clear();
				selectedRefs.addAll(availableRefs);
				notifySelectionChanged();
				checkPage();
			}
		});
		final Button unselectB = new Button(bPanel, SWT.PUSH);
		unselectB.setText(UIText.SourceBranchPage_selectNone);
		unselectB.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				for (int i = 0; i < refsTable.getItemCount(); i++)
					refsTable.getItem(i).setChecked(false);
				selectedRefs.clear();
				notifySelectionChanged();
				checkPage();
			}
		});
		bPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		addSelectionListener(new SelectionChangeListener() {
			public void selectionChanged() {
				selectB.setEnabled(selectedRefs.size() != availableRefs.size());
				unselectB.setEnabled(selectedRefs.size() != 0);
			}
		});

		setControl(panel);
		checkPage();
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible)
			revalidate();
		super.setVisible(visible);
	}

	/**
	 * Check internal state for page completion status. This method should be
	 * called only when all necessary data from previous form is available.
	 */
	private void checkPage() {
		if (transportError != null) {
			setErrorMessage(transportError);
			setPageComplete(false);
			return;
		}

		if (getSelectedBranches().isEmpty()) {
			setErrorMessage(UIText.SourceBranchPage_errorBranchRequired);
			setPageComplete(false);
			return;
		}

		setErrorMessage(null);
		setPageComplete(true);
	}

	private void revalidate() {
		if (sourcePage.selectionEquals(validatedRepoSelection)) {
			// URI hasn't changed, no need to refill the page with new data
			checkPage();
			return;
		}

		final RepositorySelection newRepoSelection = sourcePage.getSelection();
		label.setText(NLS.bind(UIText.SourceBranchPage_branchList,
				newRepoSelection.getURI().toString()));
		label.getParent().layout();

		validatedRepoSelection = null;
		transportError = null;
		head = null;
		availableRefs.clear();
		selectedRefs.clear();
		refsTable.removeAll();
		setPageComplete(false);
		setErrorMessage(null);
		label.getDisplay().asyncExec(new Runnable() {
			public void run() {
				revalidateImpl(newRepoSelection);
			}
		});
	}

	private void revalidateImpl(final RepositorySelection newRepoSelection) {
		if (label.isDisposed() || !isCurrentPage())
			return;

		final ListRemoteOperation listRemoteOp;
		try {
			final URIish uri = newRepoSelection.getURI();
			final Repository db = new Repository(new File("/tmp"));
			listRemoteOp = new ListRemoteOperation(db, uri);
			getContainer().run(true, true, listRemoteOp);
		} catch (InvocationTargetException e) {
			Throwable why = e.getCause();
			transportError(why.getMessage());
			ErrorDialog.openError(getShell(),
					UIText.SourceBranchPage_transportError,
					UIText.SourceBranchPage_cannotListBranches, new Status(
							IStatus.ERROR, Activator.getPluginId(), 0, why
									.getMessage(), why.getCause()));
			return;
		} catch (IOException e) {
			transportError(UIText.SourceBranchPage_cannotCreateTemp);
			return;
		} catch (InterruptedException e) {
			transportError(UIText.SourceBranchPage_remoteListingCancelled);
			return;
		}

		final Ref idHEAD = listRemoteOp.getRemoteRef(Constants.HEAD);
		head = null;
		for (final Ref r : listRemoteOp.getRemoteRefs()) {
			final String n = r.getName();
			if (!n.startsWith(Constants.R_HEADS))
				continue;
			availableRefs.add(r);
			if (idHEAD == null || head != null)
				continue;
			if (r.getObjectId().equals(idHEAD.getObjectId()))
				head = r;
		}
		Collections.sort(availableRefs, new Comparator<Ref>() {
			public int compare(final Ref o1, final Ref o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		if (idHEAD != null && head == null) {
			head = idHEAD;
			availableRefs.add(0, idHEAD);
		}

		validatedRepoSelection = newRepoSelection;
		for (final Ref r : availableRefs) {
			String n = r.getName();
			if (n.startsWith(Constants.R_HEADS))
				n = n.substring(Constants.R_HEADS.length());
			final TableItem ti = new TableItem(refsTable, SWT.NONE);
			ti.setText(n);
			ti.setChecked(true);
			selectedRefs.add(r);
		}
		notifySelectionChanged();
		checkPage();
	}

	private void transportError(final String msg) {
		transportError = msg;
		checkPage();
	}
}
