/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import java.util.Iterator;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIPreferences;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.revplot.PlotCommit;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevFlag;

class CommitGraphTable {
	private static Font highlightFont() {
		final Font n, h;

		n = Activator.getFont(UIPreferences.THEME_CommitGraphNormalFont);
		h = Activator.getFont(UIPreferences.THEME_CommitGraphHighlightFont);

		final FontData[] nData = n.getFontData();
		final FontData[] hData = h.getFontData();
		if (nData.length != hData.length)
			return h;
		for (int i = 0; i < nData.length; i++)
			if (!nData[i].equals(hData[i]))
				return h;

		return Activator.getBoldFont(UIPreferences.THEME_CommitGraphNormalFont);
	}

	private final TableViewer table;

	private Clipboard clipboard;

	private final SWTPlotRenderer renderer;

	private final Font nFont;

	private final Font hFont;

	private SWTCommitList allCommits;

	private RevFlag highlight;

	CommitGraphTable(final Composite parent) {
		nFont = Activator.getFont(UIPreferences.THEME_CommitGraphNormalFont);
		hFont = highlightFont();

		Table rawTable = new Table(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
		rawTable.setHeaderVisible(true);
		rawTable.setLinesVisible(false);
		rawTable.setFont(nFont);

		final TableLayout layout = new TableLayout();
		rawTable.setLayout(layout);

		createColumns(rawTable, layout);
		createPaintListener(rawTable);

		table = new TableViewer(rawTable) {
			protected Widget doFindItem(final Object element) {
				return element != null ? ((SWTCommit) element).widget : null;
			}

			protected void mapElement(final Object element, final Widget item) {
				((SWTCommit) element).widget = item;
			}
		};
		table.setLabelProvider(new GraphLabelProvider());
		table.setContentProvider(new GraphContentProvider());
		renderer = new SWTPlotRenderer(rawTable.getDisplay());

		clipboard = new Clipboard(rawTable.getDisplay());
		rawTable.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				clipboard.dispose();
			}
		});
	}

	Control getControl() {
		return table.getControl();
	}

	void selectCommit(final RevCommit c) {
		table.setSelection(new StructuredSelection(c));
		table.reveal(c);
	}

	void addSelectionChangedListener(final ISelectionChangedListener l) {
		table.addPostSelectionChangedListener(l);
	}

	void removeSelectionChangedListener(final ISelectionChangedListener l) {
		table.removePostSelectionChangedListener(l);
	}

	boolean canDoCopy() {
		return !table.getSelection().isEmpty();
	}

	void doCopy() {
		final ISelection s = table.getSelection();
		if (s.isEmpty() || !(s instanceof IStructuredSelection))
			return;
		final IStructuredSelection iss = (IStructuredSelection) s;
		final Iterator<PlotCommit> itr = iss.iterator();
		final StringBuilder r = new StringBuilder();
		while (itr.hasNext()) {
			final PlotCommit d = itr.next();
			if (r.length() > 0)
				r.append("\n");
			r.append(d.getId().name());
		}

		clipboard.setContents(new Object[] { r.toString() },
				new Transfer[] { TextTransfer.getInstance() }, DND.CLIPBOARD);
	}

	void setInput(final RevFlag hFlag, final SWTCommitList list,
			final SWTCommit[] asArray) {
		final SWTCommitList oldList = allCommits;
		highlight = hFlag;
		allCommits = list;
		table.setInput(asArray);
		if (asArray != null && asArray.length > 0) {
			if (oldList != list)
				selectCommit(asArray[0]);
		} else {
			table.getTable().deselectAll();
		}
	}

	private void createColumns(final Table rawTable, final TableLayout layout) {
		final TableColumn graph = new TableColumn(rawTable, SWT.NONE);
		graph.setResizable(true);
		graph.setText("");
		graph.setWidth(250);
		layout.addColumnData(new ColumnWeightData(20, true));

		final TableColumn author = new TableColumn(rawTable, SWT.NONE);
		author.setResizable(true);
		author.setText(UIText.HistoryPage_authorColumn);
		author.setWidth(250);
		layout.addColumnData(new ColumnWeightData(10, true));

		final TableColumn date = new TableColumn(rawTable, SWT.NONE);
		date.setResizable(true);
		date.setText(UIText.HistoryPage_dateColumn);
		date.setWidth(250);
		layout.addColumnData(new ColumnWeightData(5, true));
	}

	private void createPaintListener(final Table rawTable) {
		// Tell SWT we will completely handle painting for some columns.
		//
		rawTable.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(final Event event) {
				if (0 <= event.index && event.index <= 2)
					event.detail &= ~SWT.FOREGROUND;
			}
		});

		rawTable.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(final Event event) {
				doPaint(event);
			}
		});
	}

	void doPaint(final Event event) {
		final RevCommit c = (RevCommit) ((TableItem) event.item).getData();
		if (highlight != null && c.has(highlight))
			event.gc.setFont(hFont);
		else
			event.gc.setFont(nFont);

		if (event.index == 0) {
			renderer.paint(event);
			return;
		}

		final ITableLabelProvider lbl;
		final String txt;

		lbl = (ITableLabelProvider) table.getLabelProvider();
		txt = lbl.getColumnText(c, event.index);

		final Point textsz = event.gc.textExtent(txt);
		final int texty = (event.height - textsz.y) / 2;
		event.gc.drawString(txt, event.x, event.y + texty, true);
	}

	/**
	 * Returns the SWT Table that backs this CommitGraphTable.
	 *
	 * @return Table the SWT Table
	 */
	public Table getTable() {
		return table.getTable();
	}
}
