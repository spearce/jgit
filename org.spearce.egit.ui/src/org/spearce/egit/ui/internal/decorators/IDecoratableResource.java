/*******************************************************************************
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/

package org.spearce.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;

/**
 * Represents the state of a resource that can be used as a basis for decoration
 */
public interface IDecoratableResource {

	/**
	 * Gets the type of the resource as defined by {@link IResource}
	 *
	 * @return the type of the resource
	 */
	int getType();

	/**
	 * Gets the name of the resource
	 *
	 * @return the name of the resource
	 */
	String getName();
}
