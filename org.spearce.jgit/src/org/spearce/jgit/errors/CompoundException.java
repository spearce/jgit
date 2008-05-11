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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** An exception detailing multiple reasons for failure. */
public class CompoundException extends Exception {
	private static final long serialVersionUID = 1L;

	private static String format(final Collection<Throwable> causes) {
		final StringBuilder msg = new StringBuilder();
		msg.append("Failure due to one of the following:");
		for (final Throwable c : causes) {
			msg.append("  ");
			msg.append(c.getMessage());
			msg.append("\n");
		}
		return msg.toString();
	}

	private final List<Throwable> causeList;

	/**
	 * Constructs an exception detailing many potential reasons for failure.
	 * 
	 * @param why
	 *            Two or more exceptions that may have been the problem.
	 */
	public CompoundException(final Collection<Throwable> why) {
		super(format(why));
		causeList = Collections.unmodifiableList(new ArrayList<Throwable>(why));
	}

	/**
	 * Get the complete list of reasons why this failure happened.
	 * 
	 * @return unmodifiable collection of all possible reasons.
	 */
	public List<Throwable> getAllCauses() {
		return causeList;
	}
}
