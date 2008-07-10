/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.clone;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.transport.FetchConnection;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.URIish;

class SourceBranchPage extends WizardPage {
	private final List<BranchChangeListener> branchChangeListeners;

	private final CloneSourcePage sourcePage;

	private URIish validated;

	private Ref head;

	private List<Ref> available = Collections.<Ref> emptyList();

	private Label label;

	private Table availTable;

	private boolean allSelected;

	private String transportError;

	SourceBranchPage(final CloneSourcePage sp) {
		super(SourceBranchPage.class.getName());
		sourcePage = sp;
		setTitle(UIText.CloneSourcePage_title);
		setImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		sourcePage.addURIishChangeListener(new URIishChangeListener() {
			public void uriishChanged(final URIish newURI) {
				if (newURI == null || !newURI.equals(validated)) {
					validated = null;
					setPageComplete(false);
				}
			}
		});
		branchChangeListeners = new ArrayList<BranchChangeListener>(3);
	}

	void addBranchChangeListener(final BranchChangeListener l) {
		branchChangeListeners.add(l);
	}

	Collection<Ref> getSelectedBranches() {
		allSelected = true;
		final ArrayList<Ref> r = new ArrayList<Ref>(available.size());
		for (int i = 0; i < available.size(); i++) {
			if (availTable.getItem(i).getChecked())
				r.add(available.get(i));
			else
				allSelected = false;
		}
		return r;
	}

	Ref getHEAD() {
		return head;
	}

	boolean isAllSelected() {
		return allSelected;
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		label = new Label(panel, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		availTable = new Table(panel, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER);
		availTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		availTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				if (event.detail == SWT.CHECK) {
					notifyChanged();
					setPageComplete(isPageComplete());
				}
			}
		});

		final Composite bPanel = new Composite(panel, SWT.NONE);
		bPanel.setLayout(new RowLayout());
		Button b;
		b = new Button(bPanel, SWT.PUSH);
		b.setText(UIText.SourceBranchPage_selectAll);
		b.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing.
			}

			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < availTable.getItemCount(); i++)
					availTable.getItem(i).setChecked(true);
				notifyChanged();
				setPageComplete(isPageComplete());
			}
		});
		b = new Button(bPanel, SWT.PUSH);
		b.setText(UIText.SourceBranchPage_selectNone);
		b.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing.
			}

			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < availTable.getItemCount(); i++)
					availTable.getItem(i).setChecked(false);
				notifyChanged();
				setPageComplete(isPageComplete());
			}
		});
		bPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		setControl(panel);
		setPageComplete(false);
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible)
			revalidate();
		super.setVisible(visible);
	}

	@Override
	public boolean isPageComplete() {
		if (transportError != null) {
			setErrorMessage(transportError);
			return false;
		}

		if (getSelectedBranches().isEmpty()) {
			setErrorMessage(UIText.SourceBranchPage_errorBranchRequired);
			return false;
		}

		setErrorMessage(null);
		return true;
	}

	private void revalidate() {
		final URIish newURI;
		try {
			newURI = sourcePage.getURI();
		} catch (URISyntaxException e) {
			transportError(e.getReason());
			return;
		}

		label.setText(NLS.bind(UIText.SourceBranchPage_branchList, newURI
				.toString()));
		label.getParent().layout();

		if (newURI.equals(validated)) {
			setPageComplete(isPageComplete());
			return;
		}

		setErrorMessage(null);
		setPageComplete(false);
		transportError = null;
		head = null;
		available = new ArrayList<Ref>();
		availTable.removeAll();
		allSelected = false;
		label.getDisplay().asyncExec(new Runnable() {
			public void run() {
				revalidateImpl(newURI);
			}
		});
	}

	private void revalidateImpl(final URIish newURI) {
		if (label.isDisposed() || !isCurrentPage())
			return;
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(final IProgressMonitor pm)
						throws InvocationTargetException, InterruptedException {
					final IProgressMonitor monitor;
					if (pm == null)
						monitor = new NullProgressMonitor();
					else
						monitor = pm;
					try {
						final Repository db = new Repository(new File("/tmp"));
						final Transport tn = Transport.open(db, newURI);
						final Collection<Ref> adv;
						final FetchConnection fn = tn.openFetch();
						try {
							adv = fn.getRefs();
						} finally {
							fn.close();
							tn.close();
						}

						final Ref idHEAD = fn.getRef(Constants.HEAD);
						head = null;
						for (final Ref r : adv) {
							final String n = r.getName();
							if (!n.startsWith(Constants.HEADS_PREFIX + "/"))
								continue;
							available.add(r);
							if (idHEAD == null || head != null)
								continue;
							if (r.getObjectId().equals(idHEAD.getObjectId()))
								head = r;
						}
						Collections.sort(available, new Comparator<Ref>() {
							public int compare(final Ref o1, final Ref o2) {
								return o1.getName().compareTo(o2.getName());
							}
						});
						if (idHEAD != null && head == null) {
							head = idHEAD;
							available.add(0, idHEAD);
						}
					} catch (Exception err) {
						throw new InvocationTargetException(err);
					}
					monitor.done();
				}
			});
		} catch (InvocationTargetException e) {
			Throwable why = e.getCause();
			if ((why instanceof OperationCanceledException)) {
				transportError(UIText.SourceBranchPage_remoteListingCancelled);
				return;
			} else {
				ErrorDialog.openError(getShell(),
						UIText.SourceBranchPage_transportError,
						UIText.SourceBranchPage_cannotListBranches, new Status(
								IStatus.ERROR, Activator.getPluginId(), 0, why
										.getMessage(), why.getCause()));
				transportError(why.getMessage());
			}
			return;
		} catch (InterruptedException e) {
			transportError(UIText.SourceBranchPage_interrupted);
			return;
		}

		validated = newURI;
		allSelected = true;
		for (final Ref r : available) {
			String n = r.getName();
			if (n.startsWith(Constants.HEADS_PREFIX + "/"))
				n = n.substring((Constants.HEADS_PREFIX + "/").length());
			final TableItem ti = new TableItem(availTable, SWT.NONE);
			ti.setText(n);
			ti.setChecked(true);
		}
		notifyChanged();
		setErrorMessage(null);
		setPageComplete(isPageComplete());
	}

	private void transportError(final String msg) {
		transportError = msg;
		setErrorMessage(msg);
		setPageComplete(false);
	}

	private void notifyChanged() {
		for (final BranchChangeListener l : branchChangeListeners)
			l.branchesChanged();
	}
}