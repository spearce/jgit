/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;

/**
 * Workaround class allowing usage of clickable element as a TableViewer cell,
 * acting as button.
 * <p>
 * setValue method of EditingSupport is called on cell click, with this cell
 * editor configured.
 *
 */
public class ClickableCellEditor extends CellEditor {

	/**
	 * Create cell editor for provided table.
	 *
	 * @param table
	 *            the parent table.
	 */
	public ClickableCellEditor(final Table table) {
		super(table, SWT.NONE);
	}

	@Override
	protected Control createControl(Composite parent) {
		return null;
	}

	@Override
	protected Object doGetValue() {
		return null;
	}

	@Override
	protected void doSetFocus() {
		// nothing to do
	}

	@Override
	protected void doSetValue(Object value) {
		// nothing to do
	}

	@Override
	public void activate() {
		// just force setValue on editing support
		fireApplyEditorValue();
	}

	public void activate(ColumnViewerEditorActivationEvent activationEvent) {
		if (activationEvent.eventType != ColumnViewerEditorActivationEvent.TRAVERSAL) {
			super.activate(activationEvent);
		}
	}
}
