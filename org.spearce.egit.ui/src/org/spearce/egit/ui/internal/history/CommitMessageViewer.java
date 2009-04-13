/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
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

	private Color sys_darkgray;

	private Cursor sys_linkCursor;

	private Cursor sys_normalCursor;

	private boolean fill;

	CommitMessageViewer(final Composite parent) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		final StyledText t = getTextWidget();
		t.setFont(Activator.getFont(UIPreferences.THEME_CommitMessageFont));

		sys_linkColor = t.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		sys_darkgray = t.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
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
		setTextDoubleClickStrategy(new DefaultTextDoubleClickStrategy(),
				IDocument.DEFAULT_CONTENT_TYPE);
		activatePlugins();
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
		d.append(commit.getId().name());
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

		makeGrayText(d, styles);
		d.append("\n");
		String msg = commit.getFullMessage();
		Pattern p = Pattern.compile("\n([A-Z](?:[A-Za-z]+-)+by: [^\n]+)");
		if (fill) {
			Matcher spm = p.matcher(msg);
			if (spm.find()) {
				String subMsg = msg.substring(0, spm.end());
				msg = subMsg.replaceAll("([\\w.,; \t])\n(\\w)", "$1 $2")
						+ msg.substring(spm.end());
			}
		}
		int h0 = d.length();
		d.append(msg);

		Matcher matcher = p.matcher(msg);
		while (matcher.find()) {
			styles.add(new StyleRange(h0 + matcher.start(), matcher.end()-matcher.start(), null,  null, SWT.ITALIC));
		}

		final StyleRange[] arr = new StyleRange[styles.size()];
		styles.toArray(arr);
		setDocument(new Document(d.toString()));
		getTextWidget().setStyleRanges(arr);
	}

	private void makeGrayText(StringBuilder d,
			ArrayList<StyleRange> styles) {
		int p0 = 0;
		for (int i = 0; i<styles.size(); ++i) {
			StyleRange r = styles.get(i);
			if (p0 < r.start) {
				StyleRange nr = new StyleRange(p0, r.start  - p0, sys_darkgray, null);
				styles.add(i, nr);
				p0 = r.start;
			} else {
				if (r.foreground == null)
					r.foreground = sys_darkgray;
				p0 = r.start + r.length;
			}
		}
		if (d.length() - 1 > p0) {
			StyleRange nr = new StyleRange(p0, d.length() - p0, sys_darkgray, null);
			styles.add(nr);
		}
	}

	private void addLink(final StringBuilder d,
			final ArrayList<StyleRange> styles, final RevCommit to) {
		final ObjectLink sr = new ObjectLink();
		sr.targetCommit = to;
		sr.foreground = sys_linkColor;
		sr.underline = true;
		sr.start = d.length();
		d.append(to.getId().name());
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

	void setWrap(boolean wrap) {
		format();
		getTextWidget().setWordWrap(wrap);
	}

	void setFill(boolean fill) {
		this.fill = fill;
		format();
	}
}
