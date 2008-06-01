/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.spearce.egit.core.op.TrackOperation;

/**
 * An action to add resources to the Git repository.
 * 
 * @see TrackOperation
 */
public class Track extends RepositoryAction {

	@Override
	public void run(IAction action) {
		try {
			final TrackOperation op = new TrackOperation(Arrays
					.asList(getSelectedResources()));
			getTargetPart().getSite().getWorkbenchWindow().run(true, false,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor arg0)
								throws InvocationTargetException,
								InterruptedException {
							try {
								op.run(arg0);
							} catch (CoreException e) {
								MessageDialog.openError(getShell(),
										"Track failed", e.getMessage());
							}
						}
					});
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getShell(), "Track failed", e.getMessage());
		} catch (InterruptedException e) {
			MessageDialog.openError(getShell(), "Track failed", e.getMessage());
		}
	}

	@Override
	public boolean isEnabled() {
		return getSelectedAdaptables(getSelection(), IResource.class).length > 0;
	}
}
