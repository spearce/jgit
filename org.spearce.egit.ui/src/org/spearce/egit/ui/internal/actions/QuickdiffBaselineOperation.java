/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.internal.decorators.GitQuickDiffProvider;
import org.spearce.jgit.lib.Repository;

/**
 * UI operation to change the git quickdiff baseline
 */
public class QuickdiffBaselineOperation extends AbstractRevObjectOperation {

	private final String baseline;

	/**
	 * Construct a QuickdiffBaselineOperation for changing quickdiff baseline
	 * @param repository
	 *
	 * @param baseline
	 */
	QuickdiffBaselineOperation(final Repository repository, final String baseline) {
		super(repository);
		this.baseline = baseline;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		try {
			GitQuickDiffProvider.setBaselineReference(repository, baseline);
		} catch (IOException e) {
			Activator.logError("Cannot set quickdiff baseline", e);
		}
	}

}
