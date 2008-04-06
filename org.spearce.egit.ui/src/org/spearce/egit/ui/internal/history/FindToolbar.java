/*
 *  Copyright (C) 2008  Roger C. Soares
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.ui.internal.history;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIPreferences;
import org.spearce.egit.ui.UIText;

/**
 * A toolbar for the history page.
 *
 * @see FindToolbarThread
 * @see FindResults
 * @see GitHistoryPage
 */
public class FindToolbar extends Composite {
	private Color errorBackgroundColor;

	/**
	 * The results (matches) of the current find operation.
	 */
	public final FindResults findResults = new FindResults();

	private Preferences prefs = Activator.getDefault().getPluginPreferences();

	private List<Listener> eventList = new ArrayList<Listener>();

	private Image nextIcon;

	private Image previousIcon;

	private Table historyTable;

	private SWTCommit[] fileRevisions;

	private Text patternField;

	private Button nextButton;

	private Button previousButton;

	private Label currentPositionLabel;

	private ProgressBar progressBar;

	private String lastErrorPattern;

	/**
	 * Creates the toolbar.
	 *
	 * @param parent
	 *            the parent widget
	 */
	public FindToolbar(Composite parent) {
		super(parent, SWT.NULL);
		createToolbar();
	}

	private void createToolbar() {
		errorBackgroundColor = new Color(getDisplay(), new RGB(255, 150, 150));
		nextIcon = UIIcons.ELCL16_NEXT.createImage();
		previousIcon = UIIcons.ELCL16_PREVIOUS.createImage();

		GridLayout findLayout = new GridLayout();
		findLayout.marginHeight = 2;
		findLayout.marginWidth = 2;
		findLayout.numColumns = 8;
		setLayout(findLayout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label findLabel = new Label(this, SWT.NULL);
		findLabel.setText(UIText.HistoryPage_findbar_find);

		patternField = new Text(this, SWT.SEARCH);
		GridData findTextData = new GridData(SWT.FILL, SWT.FILL, true, false);
		findTextData.minimumWidth = 50;
		patternField.setLayoutData(findTextData);
		patternField.setText("");
		patternField.setTextLimit(100);

		nextButton = new Button(this, SWT.HORIZONTAL);
		nextButton.setImage(nextIcon);
		nextButton.setText(UIText.HistoryPage_findbar_next);

		previousButton = new Button(this, SWT.HORIZONTAL);
		previousButton.setImage(previousIcon);
		previousButton.setText(UIText.HistoryPage_findbar_previous);

		final ToolBar toolBar = new ToolBar(this, SWT.FLAT);
		new ToolItem(toolBar, SWT.SEPARATOR);

		final ToolItem prefsItem = new ToolItem(toolBar, SWT.DROP_DOWN);
		final Menu prefsMenu = new Menu(this.getShell(), SWT.POP_UP);
		final MenuItem caseItem = new MenuItem(prefsMenu, SWT.CHECK);
		caseItem.setText(UIText.HistoryPage_findbar_ignorecase);
		new MenuItem(prefsMenu, SWT.SEPARATOR);
		final MenuItem commitIdItem = new MenuItem(prefsMenu, SWT.CHECK);
		commitIdItem.setText(UIText.HistoryPage_findbar_commit);
		final MenuItem commentsItem = new MenuItem(prefsMenu, SWT.CHECK);
		commentsItem.setText(UIText.HistoryPage_findbar_comments);
		final MenuItem authorItem = new MenuItem(prefsMenu, SWT.CHECK);
		authorItem.setText(UIText.HistoryPage_findbar_author);
		final MenuItem committerItem = new MenuItem(prefsMenu, SWT.CHECK);
		committerItem.setText(UIText.HistoryPage_findbar_committer);

		prefsItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.ARROW) {
					Rectangle itemBounds = prefsItem.getBounds();
					Point point = toolBar.toDisplay(itemBounds.x, itemBounds.y
							+ itemBounds.height);
					prefsMenu.setLocation(point);
					prefsMenu.setVisible(true);
				}
			}
		});

		currentPositionLabel = new Label(this, SWT.NULL);
		GridData totalLabelData = new GridData();
		totalLabelData.horizontalAlignment = SWT.FILL;
		totalLabelData.grabExcessHorizontalSpace = true;
		currentPositionLabel.setLayoutData(totalLabelData);
		currentPositionLabel.setAlignment(SWT.RIGHT);
		currentPositionLabel.setText("");

		progressBar = new ProgressBar(this, SWT.HORIZONTAL);
		GridData findProgressBarData = new GridData();
		findProgressBarData.heightHint = 12;
		findProgressBarData.widthHint = 35;
		progressBar.setLayoutData(findProgressBarData);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);

		final FindToolbar thisToolbar = this;
		patternField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				final FindToolbarThread finder = new FindToolbarThread();
				finder.pattern = ((Text) e.getSource()).getText();
				finder.fileRevisions = fileRevisions;
				finder.toolbar = thisToolbar;
				finder.ignoreCase = caseItem.getSelection();
				finder.findInCommitId = commitIdItem.getSelection();
				finder.findInComments = commentsItem.getSelection();
				finder.findInAuthor = authorItem.getSelection();
				finder.findInCommitter = committerItem.getSelection();
				getDisplay().timerExec(200, new Runnable() {
					public void run() {
						finder.start();
					}
				});
			}
		});

		final Listener findButtonsListener = new Listener() {
			public void handleEvent(Event event) {
				if (patternField.getText().length() > 0
						&& findResults.size() == 0) {
					// If the toolbar was cleared and has a pattern typed,
					// then we redo the find with the new table data.
					final FindToolbarThread finder = new FindToolbarThread();
					finder.pattern = patternField.getText();
					finder.fileRevisions = fileRevisions;
					finder.toolbar = thisToolbar;
					finder.ignoreCase = caseItem.getSelection();
					finder.findInCommitId = commitIdItem.getSelection();
					finder.findInComments = commentsItem.getSelection();
					finder.findInAuthor = authorItem.getSelection();
					finder.findInCommitter = committerItem.getSelection();
					finder.start();
					patternField.setSelection(0, 0);
				} else {
					int currentIx = historyTable.getSelectionIndex();
					int newIx = -1;
					if (event.widget == nextButton) {
						newIx = findResults.getIndexAfter(currentIx);
						if (newIx == -1) {
							newIx = findResults.getFirstIndex();
						}
					} else {
						newIx = findResults.getIndexBefore(currentIx);
						if (newIx == -1) {
							newIx = findResults.getLastIndex();
						}
					}
					sendEvent(event.widget, newIx);

					String current = null;
					int currentValue = findResults.getMatchNumberFor(newIx);
					if (currentValue == -1) {
						current = "-";
					} else {
						current = String.valueOf(currentValue);
					}
					currentPositionLabel.setText(current + "/"
							+ findResults.size());
				}
			}
		};
		nextButton.addListener(SWT.Selection, findButtonsListener);
		previousButton.addListener(SWT.Selection, findButtonsListener);

		patternField.addKeyListener(new KeyAdapter() {
			private Event event = new Event();

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					if (nextButton.isEnabled()) {
						event.widget = nextButton;
						findButtonsListener.handleEvent(event);
					}
				} else if (e.keyCode == SWT.ARROW_UP) {
					if (previousButton.isEnabled()) {
						event.widget = previousButton;
						findButtonsListener.handleEvent(event);
					}
				}
			}
		});

		caseItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefs.setValue(UIPreferences.FINDTOOLBAR_IGNORE_CASE, caseItem
						.getSelection());
				Activator.getDefault().savePluginPreferences();
				clear();
			}
		});
		caseItem.setSelection(prefs
				.getBoolean(UIPreferences.FINDTOOLBAR_IGNORE_CASE));

		commitIdItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefs.setValue(UIPreferences.FINDTOOLBAR_COMMIT_ID,
						commitIdItem.getSelection());
				Activator.getDefault().savePluginPreferences();
				clear();
			}
		});
		commitIdItem.setSelection(prefs
				.getBoolean(UIPreferences.FINDTOOLBAR_COMMIT_ID));

		commentsItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefs.setValue(UIPreferences.FINDTOOLBAR_COMMENTS, commentsItem
						.getSelection());
				Activator.getDefault().savePluginPreferences();
				clear();
			}
		});
		commentsItem.setSelection(prefs
				.getBoolean(UIPreferences.FINDTOOLBAR_COMMENTS));

		authorItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefs.setValue(UIPreferences.FINDTOOLBAR_AUTHOR, authorItem
						.getSelection());
				Activator.getDefault().savePluginPreferences();
				clear();
			}
		});
		authorItem.setSelection(prefs
				.getBoolean(UIPreferences.FINDTOOLBAR_AUTHOR));

		committerItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefs.setValue(UIPreferences.FINDTOOLBAR_COMMITTER,
						committerItem.getSelection());
				Activator.getDefault().savePluginPreferences();
				clear();
			}
		});
		committerItem.setSelection(prefs
				.getBoolean(UIPreferences.FINDTOOLBAR_COMMITTER));
	}

	@Override
	public void dispose() {
		errorBackgroundColor.dispose();
		nextIcon.dispose();
		previousIcon.dispose();
		super.dispose();
	}

	/**
	 * Sets the table that will have its selected items changed by this toolbar.
	 * Sets the list to be searched.
	 *
	 * @param historyTable
	 * @param commitArray
	 */
	public void setInput(final Table historyTable, final SWTCommit[] commitArray) {
		this.fileRevisions = commitArray;
		this.historyTable = historyTable;
	}

	void progressUpdate(int percent) {
		int total = findResults.size();
		currentPositionLabel.setText("-/" + total);
		currentPositionLabel.setForeground(null);
		if (total > 0) {
			nextButton.setEnabled(true);
			previousButton.setEnabled(true);
			patternField.setBackground(null);
		} else {
			nextButton.setEnabled(false);
			previousButton.setEnabled(false);
		}
		progressBar.setSelection(percent);
		historyTable.clearAll();
	}

	void findCompletionUpdate(String pattern, boolean overflow) {
		int total = findResults.size();
		if (total > 0) {
			if (overflow) {
				currentPositionLabel
						.setText(UIText.HistoryPage_findbar_exceeded + " 1/"
								+ total);
			} else {
				currentPositionLabel.setText("1/" + total);
			}
			int ix = findResults.getFirstIndex();
			sendEvent(null, ix);

			patternField.setBackground(null);
			nextButton.setEnabled(true);
			previousButton.setEnabled(true);
			lastErrorPattern = null;
		} else {
			if (pattern.length() > 0) {
				patternField.setBackground(errorBackgroundColor);
				currentPositionLabel
						.setText(UIText.HistoryPage_findbar_notFound);
				// Don't keep beeping every time if the user is deleting
				// a long not found pattern
				if (lastErrorPattern == null
						|| (lastErrorPattern != null && !lastErrorPattern
								.startsWith(pattern))) {
					getDisplay().beep();
					nextButton.setEnabled(false);
					previousButton.setEnabled(false);
				}
				lastErrorPattern = pattern;
			} else {
				patternField.setBackground(null);
				currentPositionLabel.setText("");
				nextButton.setEnabled(false);
				previousButton.setEnabled(false);
				lastErrorPattern = null;
			}
		}
		progressBar.setSelection(0);
		historyTable.clearAll();

		if (overflow) {
			Display display = getDisplay();
			currentPositionLabel.setForeground(display
					.getSystemColor(SWT.COLOR_RED));
			display.beep();
		} else {
			currentPositionLabel.setForeground(null);
		}
	}

	/**
	 * Clears the toolbar.
	 */
	public void clear() {
		patternField.setBackground(null);
		if (patternField.getText().length() > 0) {
			patternField.selectAll();
			nextButton.setEnabled(true);
			previousButton.setEnabled(true);
		} else {
			nextButton.setEnabled(false);
			previousButton.setEnabled(false);
		}
		currentPositionLabel.setText("");
		progressBar.setSelection(0);
		lastErrorPattern = null;

		findResults.clear();
		if (historyTable != null) {
			historyTable.clearAll();
		}

		FindToolbarThread.updateGlobalThreadIx();
	}

	private void sendEvent(Widget widget, int index) {
		Event event = new Event();
		event.type = SWT.Selection;
		event.index = index;
		event.widget = widget;
		event.data = fileRevisions[index];
		for (Listener listener : eventList) {
			listener.handleEvent(event);
		}
	}

	/**
	 * Adds a selection event listener. The toolbar generates events when it
	 * selects an item in the history table
	 *
	 * @param listener
	 *            the listener that will receive the event
	 */
	public void addSelectionListener(Listener listener) {
		eventList.add(listener);
	}

}
