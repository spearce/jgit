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
package org.spearce.jgit.errors;

import org.spearce.jgit.revwalk.RevWalk;

/**
 * Indicates a checked exception was thrown inside of {@link RevWalk}.
 * <p>
 * Usually this exception is thrown from the Iterator created around a RevWalk
 * instance, as the Iterator API does not allow checked exceptions to be thrown
 * from hasNext() or next(). The {@link Exception#getCause()} of this exception
 * is the original checked exception that we really wanted to throw back to the
 * application for handling and recovery.
 */
public class RevWalkException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new walk exception an original cause.
	 *
	 * @param cause
	 *            the checked exception that describes why the walk failed.
	 */
	public RevWalkException(final Throwable cause) {
		super("Walk failure.", cause);
	}
}
