/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
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
