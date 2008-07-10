/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.spearce.jgit.lib.Repository;

abstract class AbstractRevObjectOperation implements IWorkspaceRunnable {

	Repository repository;

	AbstractRevObjectOperation(final Repository repository) {
		this.repository = repository;
	}

}
