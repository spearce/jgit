/*
 *  Copyright (C) 2007  Robin Rosenberg <robin.rosenberg@dewire.com>
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
package org.spearce.jgit.stgit;

import org.spearce.jgit.lib.ObjectId;

/**
 * A Stacked Git patch
 */
public class StGitPatch {

	/**
	 * Construct an StGitPatch
	 * @param patchName
	 * @param id
	 */
	public StGitPatch(String patchName, ObjectId id) {
		name = patchName;
		gitId = id;
	}

	/**
	 * @return commit id of patch
	 */
	public ObjectId getGitId() {
		return gitId;
	}

	/**
	 * @return name of patch
	 */
	public String getName() {
		return name;
	}

	private String name;
	private ObjectId gitId;
}
