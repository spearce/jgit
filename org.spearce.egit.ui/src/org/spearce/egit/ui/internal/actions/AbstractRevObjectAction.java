/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import org.spearce.egit.ui.internal.history.RevObjectSelectionProvider;
import org.spearce.jgit.lib.Repository;

abstract class AbstractRevObjectAction extends AbstractOperationAction {

	/**
	 * Find out which repository is involved here
	 *
	 * @return the Git repository associated with the selected RevObject
	 */
	protected Repository getActiveRepository() {
		RevObjectSelectionProvider selectionProvider = (RevObjectSelectionProvider) wp
				.getSite().getSelectionProvider();
		return selectionProvider.getActiveRepository();
	}

}
