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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class MappedListTest extends TestCase {

	public void testIt() {
		List<Integer> x = new MappedList<Float, Integer>(Arrays
				.asList(new Float[] { new Float(1f), new Float(2.5f),
						new Float(3.14f) })) {
			@Override
			protected Integer map(Float from) {
				return new Integer((int) from.floatValue() * 2);
			}
		};

		assertEquals(3, x.toArray().length);
		assertEquals(new Integer(2), x.toArray()[0]);
		assertEquals(new Integer(4), x.toArray()[1]);
		assertEquals(new Integer(6), x.toArray()[2]);
	}
}
