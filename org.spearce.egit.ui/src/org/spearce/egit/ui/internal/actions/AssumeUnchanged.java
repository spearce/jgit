/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.jface.action.IAction;
import org.spearce.egit.core.op.AssumeUnchangedOperation;

/**
 * This operation sets the assume-valid bit in the index for the
 * selected resources.
 *
 * @see AssumeUnchangedOperation
 */
public class AssumeUnchanged extends AbstractOperationAction {
	protected IWorkspaceRunnable createOperation(final IAction act,
			final List sel) {
		return sel.isEmpty() ? null : new AssumeUnchangedOperation(sel);
	}
}
