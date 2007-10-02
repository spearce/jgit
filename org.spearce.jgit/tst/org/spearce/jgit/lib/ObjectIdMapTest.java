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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

/**
 * Test functionality and performance.
 *
 * Performance is included because that is the very reason that
 * {@link ObjectIdMap} exists.
 *
 */
public class ObjectIdMapTest extends TestCase {

	ObjectId[] ids = new ObjectId[500000];
	
	protected void setUp() throws Exception {
		int b=0;
		for (int i=0; i<ids.length; ++i) {
			byte[] data = new byte[Constants.OBJECT_ID_LENGTH];
			for (int j=0; j<Constants.OBJECT_ID_LENGTH; ++j)
				data[j] = (byte) (b++^0xEE);
			ids[i] = new ObjectId(data);
		}
	}

	protected void tearDown() throws Exception {
		ids = null; // avoid out of memory
	}

	public void testBoth() {
		long d1=0;
		long d2=0;
		long d3=0;
		long d4=0;
		long d5=0;
		long d6=0;

		for (int j=0; j<64; ++j) {
			int x = 
				((j & 1)!=0  ? 1 : 0) |
				((j & 2)!=0  ? 2 : 0) |
				((j & 4)!=0  ? 16 : 0) |
				((j & 8)!=0  ? 32 : 0) |
				((j & 16)!=0 ? 4 : 0) |
				((j & 32)!=0 ? 8 : 0);

			if ((x&1) == 0) {
				long t0 = System.currentTimeMillis();
				
				Map treeMap = new TreeMap();
				for (int i=0; i<ids.length; ++i)
					treeMap.put(ids[i],ids[i]);
		
				long t1 = System.currentTimeMillis();
				d1 += t1-t0;
			}
			if ((x&2) == 0) {
				long t0 = System.currentTimeMillis();
				Map hashMap = new HashMap();
				for (int i=0; i<ids.length; ++i)
					hashMap.put(ids[i],ids[i]);
				long t1 = System.currentTimeMillis();
				d2 += t1-t0;
			}
			
			if ((x&4) == 0) {
				long t0= System.currentTimeMillis();
	
				Map levelMapWithTree = new ObjectIdMap(new TreeMap());
				for (int i=0; i<ids.length; ++i)
					levelMapWithTree.put(ids[i],ids[i]);
	
				long t1 = System.currentTimeMillis();
				d3 += t1-t0;
			}
			
			if ((x&8) == 0) {
				long t0 = System.currentTimeMillis();
				Map levelMapWithHash = new ObjectIdMap(new HashMap());
				for (int i=0; i<ids.length; ++i)
					levelMapWithHash.put(ids[i],ids[i]);
		
				long t1 = System.currentTimeMillis();
	
				d4 += t1-t0;
			}

			if ((x&16) == 0) {
				long t0= System.currentTimeMillis();
	
				Map levelMapWithTreeAndSpecialCompare = new ObjectIdMap(new TreeMap(new Comparator() {
				
					public int compare(Object arg0, Object arg1) {
						byte[] b0=((ObjectId)arg0).getBytes();
						byte[] b1=((ObjectId)arg1).getBytes();
						for (int i=1; i<Constants.OBJECT_ID_LENGTH; ++i) {
							int a=b0[i]&0xff;
							int b=b1[i]&0xff;
							int c=a-b;
							if (c!=0)
								return c;
						}
						return 0;
					}
				
				}));
				for (int i=0; i<ids.length; ++i)
					levelMapWithTreeAndSpecialCompare.put(ids[i],ids[i]);
	
				long t1 = System.currentTimeMillis();
				d5 += t1-t0;
			}
			
			if ((j&32) == 0) {
				long t0= System.currentTimeMillis();
	
				Map levelMapWithTreeAndSpecialCompare = new ObjectIdMap(new TreeMap(new Comparator() {
				
					public int compare(Object arg0, Object arg1) {
						return ((Comparable)arg0).compareTo(arg1);
					}
				
				}));
				for (int i=0; i<ids.length; ++i)
					levelMapWithTreeAndSpecialCompare.put(ids[i],ids[i]);
	
				long t1 = System.currentTimeMillis();
				d6 += t1-t0;
			}
		}
		
		System.out.println("TreeMap                              ="+d1);
		System.out.println("HashMap                              ="+d2);
		System.out.println("Partitioned TreeMap ObjectId.compare ="+d3);
		System.out.println("Partitioned HashMap                  ="+d4);
		System.out.println("Partitioned TreeMap enhanced compare ="+d5);
		System.out.println("Partitioned TreeMap dummy    compare ="+d6);
		assertEquals(d5*2/10000, d2/10000); // d5 is twice as fast
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
