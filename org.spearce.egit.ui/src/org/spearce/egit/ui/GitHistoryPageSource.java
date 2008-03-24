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
