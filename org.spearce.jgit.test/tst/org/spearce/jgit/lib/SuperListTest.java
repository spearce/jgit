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

public class SuperListTest extends TestCase {

	public void testEmpty() {
		List<Integer> l = new SuperList<Integer>();
		assertEquals(0, l.size());
	}

	@SuppressWarnings("boxing")
	public void testNonEmpty() {
		List<Integer> l = new SuperList<Integer>();
		List<Integer> sl1 = Arrays.asList(new Integer[] { 3,4 });
		List<Integer> sl2 = Arrays.asList(new Integer[] { });
		List<Integer> sl3 = Arrays.asList(new Integer[] { 5,6 });

		l.addAll(sl1);
		l.addAll(sl2);
		l.addAll(sl3);
		l.add(new Integer(7));

		assertEquals(5, l.size());
		assertEquals(new Integer(3), l.toArray()[0]);
		assertEquals(new Integer(4), l.toArray()[1]);
		assertEquals(new Integer(5), l.toArray()[2]);
		assertEquals(new Integer(6), l.toArray()[3]);
		assertEquals(new Integer(7), l.toArray()[4]);

		assertEquals(5, l.toArray().length);
	}
}
