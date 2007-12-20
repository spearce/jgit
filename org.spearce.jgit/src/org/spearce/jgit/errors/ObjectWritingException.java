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

/**
 * Cannot store an object in the object database. This is a serious
 * error that users need to be made aware of.
 */
public class ObjectWritingException extends IOException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an ObjectWritingException with the specified detail message.
	 *
	 * @param s message
	 */
	public ObjectWritingException(final String s) {
		super(s);
	}

	/**
	 * Constructs an ObjectWritingException with the specified detail message.
	 *
	 * @param s message
	 * @param cause root cause exception
	 */
	public ObjectWritingException(final String s, final Throwable cause) {
		super(s);
		initCause(cause);
	}
}
