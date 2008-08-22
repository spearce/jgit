/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;

/**
 * Base wizard page class for pages that need support for inter-pages
 * dependencies.
 * <p>
 * This abstract class maintains list of selection change listeners and provides
 * method to notify them about selection change.
 *
 * @see SelectionChangeListener
 */
public abstract class BaseWizardPage extends WizardPage {
	private final List<SelectionChangeListener> selectionListeners;

	/**
	 * Create base wizard with specified name. Listeners list is empty.
	 *
	 * @see WizardPage#WizardPage(String)
	 * @param pageName
	 *            page name.
	 */
	public BaseWizardPage(final String pageName) {
		super(pageName);
		selectionListeners = new LinkedList<SelectionChangeListener>();
	}

	/**
	 * Add {@link SelectionChangeListener} to list of listeners notified on
	 * selection change on this page.
	 *
	 * @param l
	 *            listener that will be notified about changes.
	 */
	public void addSelectionListener(final SelectionChangeListener l) {
		selectionListeners.add(l);
	}

	/**
	 * Notifies registered listeners about selection change.
	 */
	protected void notifySelectionChanged() {
		for (final SelectionChangeListener l : selectionListeners)
			l.selectionChanged();
	}

}
