/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
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
