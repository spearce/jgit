/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

class FileDiffLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {
	public String getColumnText(final Object element, final int columnIndex) {
		final FileDiff c = (FileDiff) element;
		switch (columnIndex) {
		case 0:
			return c.change;
		case 1:
			return c.path;
		}
		return "";
	}

	public Image getColumnImage(final Object element, final int columnIndex) {
		return null;
	}
}
