/*
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
 * are hard to make stable.
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
