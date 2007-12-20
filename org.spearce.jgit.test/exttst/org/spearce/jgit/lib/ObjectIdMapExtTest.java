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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Test ObjectIdMap performance.
 *
 * Performance is the very reason that {@link ObjectIdMap} exists.
 *
 */
public class ObjectIdMapExtTest extends TestCase {

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

	/**
	 * Test performance of {@link ObjectIdMap#put(ObjectId, Object)}
	 */
	@SuppressWarnings("unchecked")
	public void testBothPut() {
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
		assertFaster(1.2f, 3f, d5, d2);
	}

	/**
	 * Test performance of {@link ObjectIdMap#get(Object)}
	 */
	@SuppressWarnings("unchecked")
	public void testBothGet() {
		long d1=0;
		long d2=0;
		long d3=0;
		long d4=0;
		long d5=0;
		long d6=0;

		Map treeMap = new TreeMap();
		for (int i=0; i<ids.length; ++i)
			treeMap.put(ids[i],ids[i]);

		Map hashMap = new HashMap();
		for (int i=0; i<ids.length; ++i)
			hashMap.put(ids[i],ids[i]);

		Map levelMapWithTree = new ObjectIdMap(new TreeMap());
		for (int i=0; i<ids.length; ++i)
			levelMapWithTree.put(ids[i],ids[i]);

		Map levelMapWithHash = new ObjectIdMap(new HashMap());
		for (int i=0; i<ids.length; ++i)
			levelMapWithHash.put(ids[i],ids[i]);

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

		Map levelMapWithTreeAndSpecialCompare2 = new ObjectIdMap(new TreeMap(new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return ((Comparable)arg0).compareTo(arg1);
			}
		}));
		for (int i=0; i<ids.length; ++i)
			levelMapWithTreeAndSpecialCompare2.put(ids[i],ids[i]);

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

				for (int i=0; i<ids.length; ++i)
					treeMap.get(ids[i]);

				long t1 = System.currentTimeMillis();
				d1 += t1-t0;
			}
			if ((x&2) == 0) {
				long t0 = System.currentTimeMillis();
				for (int i=0; i<ids.length; ++i)
					hashMap.get(ids[i]);
				long t1 = System.currentTimeMillis();
				d2 += t1-t0;
			}

			if ((x&4) == 0) {
				long t0= System.currentTimeMillis();

				for (int i=0; i<ids.length; ++i)
					levelMapWithTree.get(ids[i]);

				long t1 = System.currentTimeMillis();
				d3 += t1-t0;
			}

			if ((x&8) == 0) {
				long t0 = System.currentTimeMillis();
				for (int i=0; i<ids.length; ++i)
					levelMapWithHash.get(ids[i]);

				long t1 = System.currentTimeMillis();

				d4 += t1-t0;
			}

			if ((x&16) == 0) {
				long t0= System.currentTimeMillis();

				for (int i=0; i<ids.length; ++i)
					levelMapWithTreeAndSpecialCompare.get(ids[i]);

				long t1 = System.currentTimeMillis();
				d5 += t1-t0;
			}

			if ((j&32) == 0) {
				long t0= System.currentTimeMillis();

				for (int i=0; i<ids.length; ++i)
					levelMapWithTreeAndSpecialCompare2.get(ids[i]);

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
		assertFaster(1.0f, 3f, d5, d2); // d5 is twice as fast
	}

	/**
	 * Test performance of {@link ObjectIdMap#keySet()} iterator
	 */
	@SuppressWarnings("unchecked")
	public void testBothIterate() {
		long d1=0;
		long d2=0;
		long d3=0;
		long d4=0;
		long d5=0;
		long d6=0;

		Map treeMap = new TreeMap();
		for (int i=0; i<ids.length; ++i)
			treeMap.put(ids[i],ids[i]);

		Map hashMap = new HashMap();
		for (int i=0; i<ids.length; ++i)
			hashMap.put(ids[i],ids[i]);

		Map levelMapWithTree = new ObjectIdMap(new TreeMap());
		for (int i=0; i<ids.length; ++i)
			levelMapWithTree.put(ids[i],ids[i]);

		Map levelMapWithHash = new ObjectIdMap(new HashMap());
		for (int i=0; i<ids.length; ++i)
			levelMapWithHash.put(ids[i],ids[i]);

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

		Map levelMapWithTreeAndSpecialCompare2 = new ObjectIdMap(new TreeMap(new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return ((Comparable)arg0).compareTo(arg1);
			}
		}));
		for (int i=0; i<ids.length; ++i)
			levelMapWithTreeAndSpecialCompare2.put(ids[i],ids[i]);

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

				for (int l=0; l<100; ++l)
					for (Iterator k=treeMap.keySet().iterator(); k.hasNext(); )
						k.next();

				long t1 = System.currentTimeMillis();
				d1 += t1-t0;
			}
			if ((x&2) == 0) {
				long t0 = System.currentTimeMillis();
				for (int l=0; l<100; ++l)
					for (Iterator k=hashMap.keySet().iterator(); k.hasNext(); )
						k.next();
				long t1 = System.currentTimeMillis();
				d2 += t1-t0;
			}

			if ((x&4) == 0) {
				long t0= System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					for (Iterator k=levelMapWithTree.keySet().iterator(); k.hasNext(); )
						k.next();

				long t1 = System.currentTimeMillis();
				d3 += t1-t0;
			}

			if ((x&8) == 0) {
				long t0 = System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					for (Iterator k=levelMapWithHash.keySet().iterator(); k.hasNext(); )
						k.next();

				long t1 = System.currentTimeMillis();

				d4 += t1-t0;
			}

			if ((x&16) == 0) {
				long t0= System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					for (Iterator k=levelMapWithTreeAndSpecialCompare.keySet().iterator(); k.hasNext(); )
						k.next();

				long t1 = System.currentTimeMillis();
				d5 += t1-t0;
			}

			if ((j&32) == 0) {
				long t0= System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					for (Iterator k=levelMapWithTreeAndSpecialCompare2.keySet().iterator(); k.hasNext(); )
						k.next();

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
		assertSlower(5f, 15f, d5, d2);
	}

	/**
	 * Test performance of {@link ObjectIdMap#values()}
	 */
	@SuppressWarnings("unchecked")
	public void testBothValues() {
		long d1=0;
		long d2=0;
		long d3=0;
		long d4=0;
		long d5=0;
		long d6=0;

		Map treeMap = new TreeMap();
		for (int i=0; i<ids.length; ++i)
			treeMap.put(ids[i],ids[i]);

		Map hashMap = new HashMap();
		for (int i=0; i<ids.length; ++i)
			hashMap.put(ids[i],ids[i]);

		Map levelMapWithTree = new ObjectIdMap(new TreeMap());
		for (int i=0; i<ids.length; ++i)
			levelMapWithTree.put(ids[i],ids[i]);

		Map levelMapWithHash = new ObjectIdMap(new HashMap());
		for (int i=0; i<ids.length; ++i)
			levelMapWithHash.put(ids[i],ids[i]);

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

		Map levelMapWithTreeAndSpecialCompare2 = new ObjectIdMap(new TreeMap(new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return ((Comparable)arg0).compareTo(arg1);
			}
		}));
		for (int i=0; i<ids.length; ++i)
			levelMapWithTreeAndSpecialCompare2.put(ids[i],ids[i]);

		for (int j=0; j<8192; ++j) {
			int x =
				((j & 1)!=0  ? 1 : 0) |
				((j & 2)!=0  ? 2 : 0) |
				((j & 4)!=0  ? 16 : 0) |
				((j & 8)!=0  ? 32 : 0) |
				((j & 16)!=0 ? 4 : 0) |
				((j & 32)!=0 ? 8 : 0);

			if ((x&1) == 0) {
				long t0 = System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					treeMap.values();

				long t1 = System.currentTimeMillis();
				d1 += t1-t0;
			}
			if ((x&2) == 0) {
				long t0 = System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					hashMap.values();

				long t1 = System.currentTimeMillis();
				d2 += t1-t0;
			}

			if ((x&4) == 0) {
				long t0= System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					levelMapWithTree.values();

				long t1 = System.currentTimeMillis();
				d3 += t1-t0;
			}

			if ((x&8) == 0) {
				long t0 = System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					levelMapWithHash.values();

				long t1 = System.currentTimeMillis();

				d4 += t1-t0;
			}

			if ((x&16) == 0) {
				long t0= System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					levelMapWithTreeAndSpecialCompare.values();

				long t1 = System.currentTimeMillis();
				d5 += t1-t0;
			}

			if ((j&32) == 0) {
				long t0= System.currentTimeMillis();

				for (int l=0; l<100; ++l)
					levelMapWithTreeAndSpecialCompare2.values();

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
		assertFaster(1.5f, 3f, d5, d2);
	}

	void assertSlower(float min,float max, long d5, long d2) {
		float ratio = d5/(float)d2;
		ratio = ((int)(ratio*10+0.5))/10.0f;
		if (ratio < min || ratio > max)
			throw new AssertionFailedError("Expected (slower) "+ min + " <= " + ratio + " <= " + max);
		StackTraceElement frame = new Throwable().getStackTrace()[1];
		System.out.println("Expected slower (ok) "+ frame.getClassName()+"."+frame.getMethodName() + " in range: " + min + " <= " + ratio + " <= " + max);
	}

	void assertFaster(float min,float max, long d5, long d2) {
		float ratio = d2/(float)d5;
		ratio = ((int)(ratio*10+0.5))/10.0f;
		if (ratio < min || ratio > max)
			throw new AssertionFailedError("Expected (faster) "+ min + " <= " + ratio + " <= " + max);
		StackTraceElement frame = new Throwable().getStackTrace()[1];
		System.out.println("Expected faster (ok) "+ frame.getClassName()+"."+frame.getMethodName() + " in range: " + min + " <= " + ratio + " <= " + max);
	}
}
