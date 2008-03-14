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
package org.spearce.jgit.treewalk.filter;

import java.io.UnsupportedEncodingException;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.treewalk.TreeWalk;

/**
 * Includes tree entries only if they match the configured path.
 * <p>
 * Applications should use {@link PathFilterGroup} to connect these into a tree
 * filter graph, as the group supports breaking out of traversal once it is
 * known the path can never match.
 */
public class PathFilter extends TreeFilter {
	/**
	 * Create a new tree filter for a user supplied path.
	 * <p>
	 * Path strings are relative to the root of the repository. If the user's
	 * input should be assumed relative to a subdirectory of the repository the
	 * caller must prepend the subdirectory's path prior to creating the filter.
	 * <p>
	 * Path strings use '/' to delimit directories on all platforms.
	 * 
	 * @param path
	 *            the path to filter on. Must not be the empty string. All
	 *            trailing '/' characters will be trimmed before string's length
	 *            is checked or is used as part of the constructed filter.
	 * @return a new filter for the requested path.
	 * @throws IllegalArgumentException
	 *             the path supplied was the empty string.
	 */
	public static PathFilter create(String path) {
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		if (path.length() == 0)
			throw new IllegalArgumentException("Empty path not permitted.");
		return new PathFilter(path);
	}

	final String pathStr;

	final byte[] pathRaw;

	private PathFilter(final String s) {
		pathStr = s;
		try {
			pathRaw = pathStr.getBytes(Constants.CHARACTER_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("JVM doesn't support "
					+ Constants.CHARACTER_ENCODING
					+ " which is required for path filtering.", uee);
		}
	}

	@Override
	public boolean include(final TreeWalk walker) {
		return walker.isPathPrefix(pathRaw, pathRaw.length) == 0;
	}

	@Override
	public boolean shouldBeRecursive() {
		for (final byte b : pathRaw)
			if (b == '/')
				return true;
		return false;
	}

	@Override
	public TreeFilter clone() {
		return this;
	}

	public String toString() {
		return "PATH(\"" + pathStr + "\")";
	}
}
