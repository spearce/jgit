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
package org.spearce.jgit.transport;

/** Specification of annotated tag behavior during fetch. */
public enum TagOpt {
	/**
	 * Automatically follow tags if we fetch the thing they point at.
	 * <p>
	 * This is the default behavior and tries to balance the benefit of having
	 * an annotated tag against the cost of possibly objects that are only on
	 * branches we care nothing about. Annotated tags are fetched only if we can
	 * prove that we already have (or will have when the fetch completes) the
	 * object the annotated tag peels (dereferences) to.
	 */
	AUTO_FOLLOW(""),

	/**
	 * Never fetch tags, even if we have the thing it points at.
	 * <p>
	 * This option must be requested by the user and always avoids fetching
	 * annotated tags. It is most useful if the location you are fetching from
	 * publishes annotated tags, but you are not interested in the tags and only
	 * want their branches.
	 */
	NO_TAGS("--no-tags"),

	/**
	 * Always fetch tags, even if we do not have the thing it points at.
	 * <p>
	 * Unlike {@link #AUTO_FOLLOW} the tag is always obtained. This may cause
	 * hundreds of megabytes of objects to be fetched if the receiving
	 * repository does not yet have the necessary dependencies.
	 */
	FETCH_TAGS("--tags");

	private final String option;

	private TagOpt(final String o) {
		option = o;
	}

	/**
	 * Get the command line/configuration file text for this value.
	 * 
	 * @return text that appears in the configuration file to activate this.
	 */
	public String option() {
		return option;
	}

	/**
	 * Convert a command line/configuration file text into a value instance.
	 * 
	 * @param o
	 *            the configuration file text value.
	 * @return the option that matches the passed parameter.
	 */
	public static TagOpt fromOption(final String o) {
		if (o == null || o.length() == 0)
			return AUTO_FOLLOW;
		for (final TagOpt tagopt : values()) {
			if (tagopt.option().equals(o))
				return tagopt;
		}
		throw new IllegalArgumentException("Invald tag option: " + o);
	}
}
