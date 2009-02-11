/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.jface.action.IAction;
import org.spearce.egit.core.op.DisconnectProviderOperation;
import org.spearce.egit.ui.internal.decorators.GitLightweightDecorator;

/**
 *	Action to disassociate a project from its Git repository.
 *
 *  @see DisconnectProviderOperation
 */
public class Disconnect extends AbstractOperationAction {
	protected IWorkspaceRunnable createOperation(final IAction act,
			final List sel) {
		return sel.isEmpty() ? null : new DisconnectProviderOperation(sel);
	}

	protected void postOperation() {
		GitLightweightDecorator.refresh();
	}
}
