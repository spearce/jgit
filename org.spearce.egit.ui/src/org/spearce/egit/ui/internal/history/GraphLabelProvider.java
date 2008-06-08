/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.revwalk.RevCommit;

class GraphLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {
	private final DateFormat fmt;

	private RevCommit lastCommit;

	private PersonIdent lastAuthor;

	GraphLabelProvider() {
		fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	public String getColumnText(final Object element, final int columnIndex) {
		final RevCommit c = (RevCommit) element;
		if (columnIndex == 0)
			return c.getShortMessage();

		final PersonIdent author = authorOf(c);
		if (author != null) {
			switch (columnIndex) {
			case 1:
				return author.getName() + " <" + author.getEmailAddress() + ">";
			case 2:
				return fmt.format(author.getWhen());
			}
		}

		return "";
	}

	private PersonIdent authorOf(final RevCommit c) {
		if (lastCommit != c) {
			lastCommit = c;
			lastAuthor = c.getAuthorIdent();
		}
		return lastAuthor;
	}

	public Image getColumnImage(final Object element, final int columnIndex) {
		return null;
	}
}
