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

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.revwalk.RevCommit;

class CommitGraphTable {
	private final TableViewer table;

	private final SWTPlotRenderer renderer;

	private SWTCommitList allCommits;

	CommitGraphTable(final Composite parent) {
		final Table rawTable = new Table(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		rawTable.setHeaderVisible(true);
		rawTable.setLinesVisible(false);

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
	}

	Control getControl() {
		return table.getControl();
	}

	void selectCommit(final RevCommit c) {
		table.setSelection(new StructuredSelection(c));
		table.reveal(c);
	}

	void addSelectionChangedListener(final ISelectionChangedListener l) {
		table.addSelectionChangedListener(l);
	}

	void removeSelectionChangedListener(final ISelectionChangedListener l) {
		table.removeSelectionChangedListener(l);
	}

	void setInput(final SWTCommitList list, final SWTCommit[] asArray) {
		final SWTCommitList oldList = allCommits;
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
			public void handleEvent(Event event) {
				if (event.index == 0)
					event.detail &= ~SWT.FOREGROUND;
			}
		});

		rawTable.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(final Event event) {
				if (event.index == 0)
					renderer.paint(event);
			}
		});
	}

}
