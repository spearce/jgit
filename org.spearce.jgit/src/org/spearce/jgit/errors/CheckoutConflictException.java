/*
 *  Copyright (C) 2007 Dave Watson <dwatson@mimvista.com>
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
 * Exception thrown if a conflict occurs during a merge checkout.
 */
public class CheckoutConflictException extends IOException {
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a CheckoutConflictException for the specified file
	 *
	 * @param file
	 */
	public CheckoutConflictException(String file) {
		super("Checkout conflict with file: " + file);
	}
	
	/**
	 * Construct a CheckoutConflictException for the specified set of files
	 *
	 * @param files
	 */
	public CheckoutConflictException(String[] files) {
		super("Checkout conflict with files: " + buildList(files));
	}

	private static String buildList(String[] files) {
		StringBuilder builder = new StringBuilder();
		for (String f : files) { 
			builder.append("\n");
			builder.append(f);
		}
		return builder.toString();
	}
}
