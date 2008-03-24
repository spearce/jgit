/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

/** A list of IResource, adaptable to the first item. */
public class ResourceList implements IAdaptable {
	private final IResource[] list;

	/**
	 * Create a new list of resources.
	 * 
	 * @param items
	 *            the items to contain in this list.
	 */
	public ResourceList(final IResource[] items) {
		list = items;
	}

	/**
	 * Get the items stored in this list.
	 * 
	 * @return the list provided to our constructor.
	 */
	public IResource[] getItems() {
		return list;
	}

	public Object getAdapter(final Class adapter) {
		if (adapter == IResource.class && list != null && list.length > 0)
			return list[0];
		return null;
	}
}
