/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.errors;

import java.io.IOException;

import org.spearce.jgit.lib.ObjectId;

/**
 * Exception thrown when an object cannot be read from Git.
 */
public class CorruptObjectException extends IOException {
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a CorruptObjectException for reporting a problem specified
	 * object id
	 *
	 * @param id
	 * @param why
	 */
	public CorruptObjectException(final ObjectId id, final String why) {
		super("Object " + id + " is corrupt: " + why);
	}

	/**
	 * Construct a CorruptObjectException for reporting a problem not associated
	 * with a specific object id.
	 *
	 * @param why
	 */
	public CorruptObjectException(final String why) {
		super(why);
	}
}
