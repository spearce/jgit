/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.push;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.spearce.egit.core.op.PushOperationResult;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.components.CenteredImageLabelProvider;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.URIish;
import org.spearce.jgit.transport.RemoteRefUpdate.Status;

/**
 * Table displaying push operation results.
 */
class PushResultTable {
	private static final int TABLE_PREFERRED_WIDTH = 650;

	private static final int TABLE_PREFERRED_HEIGHT = 300;

	private static final int COLUMN_STATUS_WEIGHT = 40;

	private static final int COLUMN_DST_WEIGHT = 40;

	private static final int COLUMN_SRC_WEIGHT = 40;

	private static final int COLUMN_MODE_WEIGHT = 15;

	private static final String IMAGE_DELETE = "MODE_DELETE";

	private static final String IMAGE_ADD = "MODE_ADD";

	private static final String COLOR_REJECTED_KEY = "REJECTED";

	private static final RGB COLOR_REJECTED = new RGB(255, 0, 0);

	private static final String COLOR_UPDATED_KEY = "UPDATED";

	private static final RGB COLOR_UPDATED = new RGB(0, 255, 0);

	private static final String COLOR_UP_TO_DATE_KEY = "UP_TO_DATE";

	private static final RGB COLOR_UP_TO_DATE = new RGB(245, 245, 245);

	private final TableViewer tableViewer;

	private final Composite tablePanel;

	private final ImageRegistry imageRegistry;

	private final ColorRegistry colorRegistry;

	PushResultTable(final Composite parent) {
		tablePanel = new Composite(parent, SWT.NONE);
		tablePanel.setLayout(new GridLayout());
		final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.heightHint = TABLE_PREFERRED_HEIGHT;
		layoutData.widthHint = TABLE_PREFERRED_WIDTH;
		tableViewer = new TableViewer(tablePanel);
		ColumnViewerToolTipSupport.enableFor(tableViewer);
		final Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		imageRegistry = new ImageRegistry(table.getDisplay());
		imageRegistry.put(IMAGE_ADD, UIIcons.ELCL16_ADD);
		imageRegistry.put(IMAGE_DELETE, UIIcons.ELCL16_DELETE);

		colorRegistry = new ColorRegistry(table.getDisplay());
		colorRegistry.put(COLOR_REJECTED_KEY, COLOR_REJECTED);
		colorRegistry.put(COLOR_UPDATED_KEY, COLOR_UPDATED);
		colorRegistry.put(COLOR_UP_TO_DATE_KEY, COLOR_UP_TO_DATE);

		tablePanel.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				imageRegistry.dispose();
			}
		});

		tableViewer.setComparer(new IElementComparer() {
			// we need this to keep refresh() working while having custom
			// equals() in PushOperationResult
			public boolean equals(Object a, Object b) {
				return a == b;
			}

			public int hashCode(Object element) {
				return element.hashCode();
			}
		});
		tableViewer.setContentProvider(new RefUpdateContentProvider());
		tableViewer.setInput(null);
	}

	void setData(final Repository localDb, final PushOperationResult result) {
		// We have to recreate columns.
		for (final TableColumn tc : tableViewer.getTable().getColumns())
			tc.dispose();
		// Set empty result for a while.
		tableViewer.setInput(null);

		// Layout should be recreated to work properly.
		final TableColumnLayout layout = new TableColumnLayout();
		tablePanel.setLayout(layout);

		final TableViewerColumn modeViewer = createColumn(layout,
				UIText.PushResultTable_columnMode, COLUMN_MODE_WEIGHT,
				SWT.CENTER);
		modeViewer.setLabelProvider(new CenteredImageLabelProvider() {
			@Override
			public Image getImage(Object element) {
				if (((RefUpdateElement) element).isDelete())
					return imageRegistry.get(IMAGE_DELETE);
				return imageRegistry.get(IMAGE_ADD);
			}

			@Override
			public String getToolTipText(Object element) {
				if (((RefUpdateElement) element).isDelete())
					return UIText.RefSpecPanel_modeDeleteDescription;
				return UIText.RefSpecPanel_modeUpdateDescription;
			}
		});

		final TableViewerColumn srcViewer = createColumn(layout,
				UIText.PushResultTable_columnSrc, COLUMN_SRC_WEIGHT, SWT.LEFT);
		srcViewer.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((RefUpdateElement) element).getSrcRefName();
			}
		});

		final TableViewerColumn dstViewer = createColumn(layout,
				UIText.PushResultTable_columnDst, COLUMN_DST_WEIGHT, SWT.LEFT);
		dstViewer.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((RefUpdateElement) element).getDstRefName();
			}
		});

		if (result == null) {
			tablePanel.layout();
			return;
		}

		int i = 0;
		for (final URIish uri : result.getURIs()) {
			final TableViewerColumn statusViewer = createColumn(layout, NLS
					.bind(UIText.PushResultTable_columnStatusRepo, Integer
							.toString(++i)), COLUMN_STATUS_WEIGHT, SWT.CENTER);
			statusViewer.getColumn().setToolTipText(uri.toString());
			statusViewer.setLabelProvider(new UpdateStatusLabelProvider(
					localDb, uri));
		}
		tableViewer.setInput(result);
		tablePanel.layout();
	}

	Control getControl() {
		return tablePanel;
	}

	private TableViewerColumn createColumn(
			final TableColumnLayout columnLayout, final String text,
			final int weight, final int style) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				tableViewer, style);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(text);
		columnLayout.setColumnData(column, new ColumnWeightData(weight));
		return viewerColumn;
	}

	private class UpdateStatusLabelProvider extends ColumnLabelProvider {
		private Repository localDb;

		private final URIish uri;

		UpdateStatusLabelProvider(final Repository localDb, final URIish uri) {
			this.localDb = localDb;
			this.uri = uri;
		}

		@Override
		public String getText(Object element) {
			final RefUpdateElement rue = (RefUpdateElement) element;
			if (!rue.isSuccessfulConnection(uri))
				return UIText.PushResultTable_statusConnectionFailed;

			final RemoteRefUpdate rru = rue.getRemoteRefUpdate(uri);
			switch (rru.getStatus()) {
			case OK:
				if (rru.isDelete())
					return UIText.PushResultTable_statusOkDeleted;

				final Ref oldRef = rue.getAdvertisedRemoteRef(uri);
				if (oldRef == null) {
					if (rue.getDstRefName().startsWith(Constants.R_TAGS))
						return UIText.PushResultTable_statusOkNewTag;
					return UIText.PushResultTable_statusOkNewBranch;
				}

				return oldRef.getObjectId().abbreviate(localDb).name()
						+ (rru.isFastForward() ? ".." : "...")
						+ rru.getNewObjectId().abbreviate(localDb).name();
			case UP_TO_DATE:
				return UIText.PushResultTable_statusUpToDate;
			case NON_EXISTING:
				return UIText.PushResultTable_statusNoMatch;
			case REJECTED_NODELETE:
			case REJECTED_NONFASTFORWARD:
			case REJECTED_REMOTE_CHANGED:
				return UIText.PushResultTable_statusRejected;
			case REJECTED_OTHER_REASON:
				return UIText.PushResultTable_statusRemoteRejected;
			default:
				throw new IllegalArgumentException(NLS.bind(
						UIText.PushResultTable_statusUnexpected, rru
								.getStatus()));
			}
		}

		@Override
		public Color getBackground(Object element) {
			final RefUpdateElement rue = (RefUpdateElement) element;
			if (!rue.isSuccessfulConnection(uri))
				return colorRegistry.get(COLOR_REJECTED_KEY);

			final Status status = rue.getRemoteRefUpdate(uri).getStatus();
			switch (status) {
			case OK:
				return colorRegistry.get(COLOR_UPDATED_KEY);
			case UP_TO_DATE:
			case NON_EXISTING:
				return colorRegistry.get(COLOR_UP_TO_DATE_KEY);
			case REJECTED_NODELETE:
			case REJECTED_NONFASTFORWARD:
			case REJECTED_REMOTE_CHANGED:
			case REJECTED_OTHER_REASON:
				return colorRegistry.get(COLOR_REJECTED_KEY);
			default:
				throw new IllegalArgumentException(NLS.bind(
						UIText.PushResultTable_statusUnexpected, status));
			}
		}

		@Override
		public String getToolTipText(Object element) {
			final RefUpdateElement rue = (RefUpdateElement) element;
			if (!rue.isSuccessfulConnection(uri))
				return rue.getErrorMessage(uri);

			final RemoteRefUpdate rru = rue.getRemoteRefUpdate(uri);
			final Ref oldRef = rue.getAdvertisedRemoteRef(uri);
			switch (rru.getStatus()) {
			case OK:
				if (rru.isDelete())
					return NLS.bind(UIText.PushResultTable_statusDetailDeleted,
							oldRef.getObjectId().abbreviate(localDb).name());
				if (oldRef == null)
					return null;
				if (rru.isFastForward())
					return UIText.PushResultTable_statusDetailFastForward;
				return UIText.PushResultTable_statusDetailForcedUpdate;
			case UP_TO_DATE:
				return null;
			case NON_EXISTING:
				return UIText.PushResultTable_statusDetailNonExisting;
			case REJECTED_NODELETE:
				return UIText.PushResultTable_statusDetailNoDelete;
			case REJECTED_NONFASTFORWARD:
				return UIText.PushResultTable_statusDetailNonFastForward;
			case REJECTED_REMOTE_CHANGED:
				final Ref remoteRef = oldRef;
				final String curVal;
				if (remoteRef == null)
					curVal = UIText.PushResultTable_refNonExisting;
				else
					curVal = remoteRef.getObjectId().abbreviate(localDb).name();

				final ObjectId expectedOldObjectId = rru
						.getExpectedOldObjectId();
				final String expVal;
				if (expectedOldObjectId.equals(ObjectId.zeroId()))
					expVal = UIText.PushResultTable_refNonExisting;
				else
					expVal = expectedOldObjectId.abbreviate(localDb).name();
				return NLS.bind(UIText.PushResultTable_statusDetailChanged,
						curVal, expVal);
			case REJECTED_OTHER_REASON:
				return rru.getMessage();
			default:
				throw new IllegalArgumentException(NLS.bind(
						UIText.PushResultTable_statusUnexpected, rru
								.getStatus()));
			}
		}
	}
}
