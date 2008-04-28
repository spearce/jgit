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

import java.io.IOException;

/**
 * Indicates a protocol error has occurred while fetching/pushing objects.
 */
public class TransportException extends IOException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an TransportException with the specified detail message.
	 * 
	 * @param s
	 *            message
	 */
	public TransportException(final String s) {
		super(s);
	}

	/**
	 * Constructs an TransportException with the specified detail message.
	 * 
	 * @param s
	 *            message
	 * @param cause
	 *            root cause exception
	 */
	public TransportException(final String s, final Throwable cause) {
		super(s);
		initCause(cause);
	}
}
