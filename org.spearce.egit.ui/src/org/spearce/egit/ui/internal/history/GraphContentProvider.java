/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

class GraphContentProvider implements IStructuredContentProvider {
	private SWTCommit[] list;

	public void inputChanged(final Viewer newViewer, final Object oldInput,
			final Object newInput) {
		list = (SWTCommit[]) newInput;
	}

	public Object[] getElements(final Object inputElement) {
		return list;
	}

	public void dispose() {
		// Nothing.
	}
}
