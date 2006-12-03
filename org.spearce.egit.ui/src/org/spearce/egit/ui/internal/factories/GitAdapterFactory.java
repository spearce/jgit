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
package org.spearce.egit.ui.internal.factories;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.spearce.egit.ui.GitHistoryPageSource;

public class GitAdapterFactory implements IAdapterFactory {

    private Object historyPageSource = new GitHistoryPageSource();

    public Object getAdapter(Object adaptableObject, Class adapterType) {
	if (adaptableObject instanceof File) {
	    if (adapterType.isAssignableFrom(IHistoryPageSource.class)) {
		return historyPageSource;
	    }
	}
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
