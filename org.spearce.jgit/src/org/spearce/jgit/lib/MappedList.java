/*
 *  Copyright (C) 2007  Robin Rosenberg
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
package org.spearce.jgit.lib;

import java.util.AbstractList;
import java.util.List;

/**
 * A mapping of a list from one set of values to another set whose elements
 * are a function of the input elements.
 *
 * @author Robin Rosenberg
 *
 * @param <FROM> The type of the input elements
 * @param <TO> The type of the output elements
 */
public abstract class MappedList<FROM, TO> extends AbstractList<TO> {

	private final List<FROM> input;

	/**
	 * Map the input element to another. This method is invoked to translate
	 * each element. This method may be invoked multiple times for the same
	 * element.
	 *
	 * @param from The value to map from
	 * @return another value which is a function of the input parameter.
	 */
	abstract protected TO map(FROM from);

	/** Construct a list mapped on top of an underlying list
	 *
	 * @param input The list whose elements should be mapped
	 */
	public MappedList(List<FROM> input) {
		this.input = input;
	}

	public TO get(int index) {
		return map(input.get(index));
	}

	public int size() {
		return input.size();
	}

}
