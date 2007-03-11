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

public class GitHistoryPageSource extends HistoryPageSource {

	public Page createPage(Object object) {
		return new GitHistoryPage(object);
	}

	public boolean canShowHistoryFor(Object object) {
		return (object instanceof IResource 
				&& (((IResource) object).getType() == IResource.FILE
						|| ((IResource) object).getType() == IResource.FOLDER
						|| ((IResource) object).getType() == IResource.PROJECT));
	}

}
