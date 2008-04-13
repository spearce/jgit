/*
 *  Copyright (C) 2008	Robin Rosenberg
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
 * This signals a revision or object reference was not
 * properly formatted.
 */
public class RevisionSyntaxException extends IOException {

	private final String revstr;

	/**
	 * Construct a RevisionSyntaxException indicating a syntax problem with a
	 * revision (or object) string.
	 * 
	 * @param revstr The problematic revision string
	 */
	public RevisionSyntaxException(String revstr) {
		this.revstr = revstr;
	}
	
	/**
	 * Construct a RevisionSyntaxException indicating a syntax problem with a
	 * revision (or object) string.
	 *
	 * @param message a specific reason
	 * @param revstr The problematic revision string
	 */
	public RevisionSyntaxException(String message, String revstr) {
		super(message);
		this.revstr = revstr;
	}

	@Override
	public String toString() {
		return super.toString() + ":" + revstr;
	}
}
