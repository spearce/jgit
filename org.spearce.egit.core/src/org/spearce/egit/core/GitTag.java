/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core;

import org.eclipse.team.core.history.ITag;

/**
 * A representation of a Git tag in Eclipse.
 */
public class GitTag implements ITag {

	private String name;

	/**
	 * Construct a GitTag object with a given name.
	 *
	 * @param name the Git tag name
	 */
	public GitTag(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
