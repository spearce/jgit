/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.spearce.jgit.lib.Repository;

/**
 * A selection provider for Git revision objects
 */
public class RevObjectSelectionProvider implements ISelectionProvider {

	private List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();

	private ISelection selection;

	private Repository repository;

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	public ISelection getSelection() {
		return selection;
	}

	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
		SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
		for (ISelectionChangedListener l : listeners) {
			l.selectionChanged(event);
		}
	}

	/**
	 * Sets the active repository. This one is called by the view when the view
	 * is updated with new data.
	 *
	 * @param repository
	 */
	public void setActiveRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * @return currently active repository
	 */
	public Repository getActiveRepository() {
		return repository;
	}
}
