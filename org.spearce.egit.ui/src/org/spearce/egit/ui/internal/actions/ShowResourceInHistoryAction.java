/*******************************************************************************
 * Copyright (C) 2007, David Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Guilhem Bonnefille <guilhem.bonnefille@gmail.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.HistoryPageSaveablePart;
import org.spearce.egit.core.ResourceList;

/**
 * An action to update the history view for the selected
 * resource. If the history view is not visible it will be
 * shown.
 */
public class ShowResourceInHistoryAction extends TeamAction {

	// There are changes in Eclipse 3.3 requiring that execute be implemented
	// for it to compile. while 3.2 requires that run is implemented instead.
	/**
	 * See {@link #run}
	 *
	 * @param action
	 */
	public void execute(IAction action) {
		run(action);
	}

	@Override
	public void run(IAction action) {
		TeamUI.getHistoryView().showHistoryFor(
				new ResourceList(getSelectedResources()));
	}

	void showCompareInDialog(Shell shell, Object object) {
		HistoryPageSaveablePart.showHistoryInDialog(shell, object);
	}

	public boolean isEnabled() {
		return !getSelection().isEmpty();
	}
}
