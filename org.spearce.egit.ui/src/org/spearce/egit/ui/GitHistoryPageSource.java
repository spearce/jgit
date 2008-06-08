/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;
import org.spearce.egit.core.ResourceList;
import org.spearce.egit.ui.internal.history.GitHistoryPage;

/**
 * A helper class for constructing the {@link GitHistoryPage}.
 */
public class GitHistoryPageSource extends HistoryPageSource {
	public boolean canShowHistoryFor(final Object object) {
		return GitHistoryPage.canShowHistoryFor(object);
	}

	public Page createPage(final Object object) {
		final ResourceList input;

		if (object instanceof ResourceList)
			input = (ResourceList) object;
		else if (object instanceof IResource)
			input = new ResourceList(new IResource[] { (IResource) object });
		else
			input = new ResourceList(new IResource[0]);

		final GitHistoryPage pg = new GitHistoryPage();
		pg.setInput(input);
		return pg;
	}
}
