/*
 *  Copyright (C) 2006  Robin Rosenberg
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
package org.spearce.egit.ui.internal.actions;

import java.util.Arrays;
import java.util.Hashtable;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.HistoryPageSaveablePart;

/**
 *	Compare the resources filtered in the history view with the current
 *	revision.
 */
public class CompareWithRevisionAction extends TeamAction {

	// There are changes in Eclipse 3.3 requiring that execute be implemented
	// for it to compile. while 3.2 requires that run is implemented instead.
	/** See {@link #run}
	 * @param action
	 */
	public void execute(IAction action) {
		run(action);
	}

	@Override
	public void run(IAction action) {
		super.run(action);
		System.out.println("Run:" + action);
		System.out.println("Selection resources:"
				+ Arrays.asList(getSelectedResources()));
		IResource[] r = getSelectedResources();
		Hashtable providerMapping = this.getProviderMapping(r);
		System.out.println("Mapping:" + providerMapping);
		TeamUI.getHistoryView().showHistoryFor(getSelectedResources()[0]);

	}

	void showCompareInDialog(Shell shell, Object object) {
		HistoryPageSaveablePart.showHistoryInDialog(shell, object);
	}

	public boolean isEnabled() {
		return !getSelection().isEmpty();
	}

}
