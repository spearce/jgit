/*
 *  Copyright (C) 2006  Robin Rosenberg <robin.rosenberg@dewire.com>
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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

/**
 * Test functionality of ObjectIdMap
 *
 * The reason this class exists is performance, but those figures
 * are hard to make stable. See {@link ObjectIdMapExtTest} for
 * peformance tests.
 */
public class ObjectIdMapTest extends TestCase {

	ObjectId[] ids = new ObjectId[500];
	
	protected void setUp() throws Exception {
		int b=0;
		for (int i=0; i<ids.length; ++i) {
			byte[] data = new byte[Constants.OBJECT_ID_LENGTH];
			for (int j=0; j<Constants.OBJECT_ID_LENGTH; ++j)
				data[j] = (byte) (b++^0xEE);
			ids[i] = ObjectId.fromRaw(data);
		}
	}

	protected void tearDown() throws Exception {
		ids = null; // avoid out of memory
	}

	/**
	 * Verify that ObjectIdMap and TreeMap functionally are equivalent.
	 */
	@SuppressWarnings("unchecked")
	public void testFunc() {
		Map treeMap = new TreeMap();
		for (int i=0; i<ids.length/100; ++i)
			treeMap.put(ids[i],ids[i]);
		Map levelMapWithTree = new ObjectIdMap(new TreeMap());
		for (int i=0; i<ids.length/100; ++i)
			levelMapWithTree.put(ids[i],ids[i]);
		
		assertEquals(treeMap, levelMapWithTree);
		assertEquals(treeMap.values(), levelMapWithTree.values());
		assertEquals(treeMap.keySet(), levelMapWithTree.keySet());

		treeMap.remove(ids[30]);
		levelMapWithTree.remove(ids[30]);
		assertFalse(treeMap.containsKey(ids[30]));
		assertFalse(levelMapWithTree.containsKey(ids[30]));
		assertEquals(treeMap.values(), levelMapWithTree.values());
		assertEquals(treeMap.keySet(), levelMapWithTree.keySet());
	}

	void assertEquals(Collection a, Collection b) {
		Object[] aa = a.toArray();
		Object[] ba = b.toArray();
		Arrays.sort(aa);
		Arrays.sort(ba);
		assertTrue(Arrays.equals(aa, ba));
	}

}
