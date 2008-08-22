/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.factories;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.spearce.egit.ui.GitHistoryPageSource;

/**
 * This class is an intelligent "cast" operation for getting
 * an instance of a suitable object from another for a specific
 * purpose.
 */
public class GitAdapterFactory implements IAdapterFactory {

	private Object historyPageSource = new GitHistoryPageSource();

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType.isAssignableFrom(IHistoryPageSource.class)) {
			return historyPageSource;
		}
		return null;
	}

	public Class[] getAdapterList() {
		// TODO Auto-generated method stub
		return null;
	}

}
