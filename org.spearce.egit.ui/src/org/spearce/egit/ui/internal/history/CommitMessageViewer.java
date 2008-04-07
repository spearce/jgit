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
import java.util.ArrayList;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIPreferences;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.revplot.PlotCommit;
import org.spearce.jgit.revwalk.RevCommit;

class CommitMessageViewer extends TextViewer {
	private final ListenerList navListeners = new ListenerList();

	private final DateFormat fmt;

	private PlotCommit<?> commit;

	private Color sys_linkColor;

	private Cursor sys_linkCursor;

	private Cursor sys_normalCursor;

	CommitMessageViewer(final Composite parent) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		final StyledText t = getTextWidget();
		t.setFont(Activator.getFont(UIPreferences.THEME_CommitMessageFont));

		sys_linkColor = t.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		sys_linkCursor = t.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
		sys_normalCursor = t.getCursor();

		t.addListener(SWT.MouseMove, new Listener() {
			public void handleEvent(final Event e) {
				final int o;
				try {
					o = t.getOffsetAtLocation(new Point(e.x, e.y));
				} catch (IllegalArgumentException err) {
					t.setCursor(sys_normalCursor);
					return;
				}

				final StyleRange r = t.getStyleRangeAtOffset(o);
				if (r instanceof ObjectLink)
					t.setCursor(sys_linkCursor);
				else
					t.setCursor(sys_normalCursor);
			}
		});
		t.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				final int o;
				try {
					o = t.getOffsetAtLocation(new Point(e.x, e.y));
				} catch (IllegalArgumentException err) {
					return;
				}

				final StyleRange r = t.getStyleRangeAtOffset(o);
				if (r instanceof ObjectLink) {
					final RevCommit c = ((ObjectLink) r).targetCommit;
					for (final Object l : navListeners.getListeners())
						((CommitNavigationListener) l).showCommit(c);
				}
			}
		});
	}

	void addCommitNavigationListener(final CommitNavigationListener l) {
		navListeners.add(l);
	}

	void removeCommitNavigationListener(final CommitNavigationListener l) {
		navListeners.remove(l);
	}

	@Override
	public void setInput(final Object input) {
		commit = (PlotCommit<?>) input;
		format();
	}

	public Object getInput() {
		return commit;
	}

	private void format() {
		if (commit == null) {
			setDocument(new Document());
			return;
		}

		final PersonIdent author = commit.getAuthorIdent();
		final PersonIdent committer = commit.getCommitterIdent();
		final StringBuilder d = new StringBuilder();
		final ArrayList<StyleRange> styles = new ArrayList<StyleRange>();

		d.append("commit ");
		d.append(commit.getId());
		d.append("\n");

		if (author != null) {
			d.append("Author: ");
			d.append(author.getName());
			d.append(" <");
			d.append(author.getEmailAddress());
			d.append("> ");
			d.append(fmt.format(author.getWhen()));
			d.append("\n");
		}

		if (committer != null) {
			d.append("Committer: ");
			d.append(committer.getName());
			d.append(" <");
			d.append(committer.getEmailAddress());
			d.append("> ");
			d.append(fmt.format(committer.getWhen()));
			d.append("\n");
		}

		for (int i = 0; i < commit.getParentCount(); i++) {
			final RevCommit p = commit.getParent(i);
			d.append("Parent: ");
			addLink(d, styles, p);
			d.append(" (");
			d.append(p.getShortMessage());
			d.append(")");
			d.append("\n");
		}

		for (int i = 0; i < commit.getChildCount(); i++) {
			final RevCommit p = commit.getChild(i);
			d.append("Child:  ");
			addLink(d, styles, p);
			d.append(" (");
			d.append(p.getShortMessage());
			d.append(")");
			d.append("\n");
		}

		d.append("\n  ");
		d.append(commit.getFullMessage().replaceAll("\n", "\n  "));

		final StyleRange[] arr = new StyleRange[styles.size()];
		styles.toArray(arr);
		setDocument(new Document(d.toString()));
		getTextWidget().setStyleRanges(arr);
	}

	private void addLink(final StringBuilder d,
			final ArrayList<StyleRange> styles, final RevCommit to) {
		final ObjectLink sr = new ObjectLink();
		sr.targetCommit = to;
		sr.foreground = sys_linkColor;
		sr.underline = true;
		sr.start = d.length();
		d.append(to.getId().toString());
		sr.length = d.length() - sr.start;
		styles.add(sr);
	}

	static class ObjectLink extends StyleRange {
		RevCommit targetCommit;

		public boolean similarTo(final StyleRange style) {
			if (!(style instanceof ObjectLink))
				return false;
			if (targetCommit != ((ObjectLink) style).targetCommit)
				return false;
			return super.similarTo(style);
		}
	}
}
