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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.spearce.jgit.lib.TopologicalSorter.Edge;

import junit.framework.TestCase;

public class TopologicalSorterTest extends TestCase {

	class Data implements Comparable<Data> {
		String name;

		int n;

		Data(String name, int n) {
			this.name = name;
			this.n = n;
		}

		public int compareTo(Data o) {
			int c = n - o.n;
			if (c != 0)
				return c;
			if (this == o)
				return 0;
			return name.compareTo(o.name);
		}

		@Override
		public String toString() {
			return name + "(" + n + ")";
		}
	}

	private Edge<Data> newEdge(Data a, Data b) {
		return new TopologicalSorter.Edge<Data>(a, b);
	}

	private TopologicalSorter<Data> newSorter() {
		final TopologicalSorter<Data> counter = new TopologicalSorter<Data>();
		counter.setComparator(new Comparator<Data>() {
					public int compare(Data o1, Data o2) {
						return o1.compareTo(o2);
					}
				});
		return counter;
	}

	public void testEmpty() {
		final TopologicalSorter<Data> counter = newSorter();
		assertFalse(counter.getEntries().iterator().hasNext());
	}

	public void testOneElement() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",1);
		counter.put(a);
		assertSame(a, counter.getEntries().get(0));
		assertEquals(1, counter.getEntries().size());
	}

	public void testTwoDisjointElements_0() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",1);
		Data b = new Data("b",2);
		counter.put(a);
		counter.put(b);
		assertSame(a, counter.getEntries().get(0));
		assertSame(b, counter.getEntries().get(1));
		assertEquals(2, counter.getEntries().size());
	}

	public void testTwoDisjointElements_1() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",1);
		Data b = new Data("b",2);
		// order does not matter
		counter.put(b);
		counter.put(a);
		assertSame(a, counter.getEntries().get(0));
		assertSame(b, counter.getEntries().get(1));
		assertEquals(2, counter.getEntries().size());
	}

	public void testTwoDisjointElements_2() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",1);
		// the sorting works
		counter.put(a);
		counter.put(b);
		assertSame(b, counter.getEntries().get(0));
		assertSame(a, counter.getEntries().get(1));
		assertEquals(2, counter.getEntries().size());
	}

	public void testThreeDotLine_0() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",1);
		Data c = new Data("c",3);
		counter.put(newEdge(a,b));
		counter.put(newEdge(b,c));
		assertSame(a, counter.getEntries().get(0));
		assertSame(b, counter.getEntries().get(1));
		assertSame(c, counter.getEntries().get(2));
		assertEquals(3, counter.getEntries().size());
	}

	public void testThreeDotLine_1() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",1);
		Data c = new Data("c",3);
		// put order does not matter
		counter.put(newEdge(b,c));
		counter.put(newEdge(a,b));
		assertSame(a, counter.getEntries().get(0));
		assertSame(b, counter.getEntries().get(1));
		assertSame(c, counter.getEntries().get(2));
		assertEquals(3, counter.getEntries().size());
	}

	public void testTwoDisjointLines_1() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",4);
		Data c = new Data("c",1);
		Data d = new Data("d",1);
		counter.put(newEdge(a,b));
		counter.put(newEdge(c,d));
		assertSame(c, counter.getEntries().get(0));
		assertSame(d, counter.getEntries().get(1));
		assertSame(a, counter.getEntries().get(2));
		assertSame(b, counter.getEntries().get(3));
		assertEquals(4, counter.getEntries().size());
	}

	public void testTwoDisjointLines_2() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",4);
		Data c = new Data("c",3);
		Data d = new Data("d",1);
		counter.put(newEdge(a,b));
		counter.put(newEdge(c,d));
		assertSame(a, counter.getEntries().get(0));
		assertSame(c, counter.getEntries().get(1));
		assertSame(d, counter.getEntries().get(2));
		assertSame(b, counter.getEntries().get(3));
		assertEquals(4, counter.getEntries().size());
	}

	public void testOneToTwo() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",3);
		Data c = new Data("c",2);
		counter.put(newEdge(a,b));
		counter.put(newEdge(a,c));
		assertSame(a, counter.getEntries().get(0));
		assertSame(c, counter.getEntries().get(1));
		assertSame(b, counter.getEntries().get(2));
		assertEquals(3, counter.getEntries().size());
	}

	public void testTwoToOne() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",3);
		Data c = new Data("c",1);
		counter.put(newEdge(a,b));
		counter.put(newEdge(c,b));
		assertSame(c, counter.getEntries().get(0));
		assertSame(a, counter.getEntries().get(1));
		assertSame(b, counter.getEntries().get(2));
		assertEquals(3, counter.getEntries().size());
	}

	public void testShapeDiamond() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",3);
		Data c = new Data("c",1);
		Data d = new Data("d",2);
		counter.put(newEdge(a,b));
		counter.put(newEdge(a,c));
		counter.put(newEdge(b,d));
		counter.put(newEdge(c,d));
		assertSame(a, counter.getEntries().get(0));
		assertSame(c, counter.getEntries().get(1));
		assertSame(b, counter.getEntries().get(2));
		assertSame(d, counter.getEntries().get(3));
		assertEquals(4, counter.getEntries().size());
	}

	public void testShapeEx() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",3);
		Data c = new Data("c",1);
		Data d = new Data("d",4);
		Data e = new Data("e",2);
		counter.put(newEdge(a,c));
		counter.put(newEdge(b,c));
		counter.put(newEdge(c,d));
		counter.put(newEdge(c,e));
		assertSame(a, counter.getEntries().get(0));
		assertSame(b, counter.getEntries().get(1));
		assertSame(c, counter.getEntries().get(2));
		assertSame(e, counter.getEntries().get(3));
		assertSame(d, counter.getEntries().get(4));
		assertEquals(5, counter.getEntries().size());
	}

	public void testWithFilter() {
		Data a = new Data("a", 5);

		TopologicalSorter<Data> counter = new TopologicalSorter<Data>(
				new Comparator<Data>() {
					public int compare(Data o1, Data o2) {
						return o1.compareTo(o2);
					}

				}) {
			@Override
			protected boolean filter(Data element) {
				if (element.name.equals("a"))
					return false;
				return true;
			}

			@Override
			public int size() {
				return super.size() - 1;
			}
		};
		counter.put(a);
		assertEquals(0, counter.getEntries().toArray().length);
	}

	/** A more complex case */
	public void testSort() {
		/*
		 * This is the test input. A list of nodes. The number is a weight that
		 * is used when the output order cannot be determined conclusively from
		 * the order of the nodes. Think of the number as a date in a git
		 * commit. The elements are sorted by weight (the number in parentheses)
		 * when the topological order is non defined.
		 *
		 * a(1) - b(3) - c(7) - d(10) - e(21) - f(3) - g(1) - h(1) - i(1) - j(1) -
		 *         \                 /                                    /
		 *          k(3)    -    l(2) - m(11) - n(4) - o(5) - p(5)  -  q(5)
		 *                                \                /
		 *                                 x(6) - y(5) - z(f)
		 */
		Data a = new Data("a", 1);
		Data b = new Data("b", 3);
		Data c = new Data("c", 7);
		Data d = new Data("d", 10);
		Data e = new Data("e", 21);
		Data f = new Data("f", 3);
		Data g = new Data("g", 1);
		Data h = new Data("h", 1);
		Data i = new Data("i", 1);
		Data j = new Data("j", 1);
		Data k = new Data("k", 3);
		Data l = new Data("l", 2);
		Data m = new Data("m", 11);
		Data n = new Data("n", 4);
		Data o = new Data("o", 5);
		Data p = new Data("p", 5);
		Data q = new Data("q", 5);
		Data x = new Data("x", 6);
		Data y = new Data("y", 5);
		Data z = new Data("z", 5);

		final TopologicalSorter<Data> counter = newSorter();
		counter.put(newEdge(a, b));
		counter.put(newEdge(b, c));
		counter.put(newEdge(c, d));
		counter.put(newEdge(d, e));
		counter.put(newEdge(e, f));
		counter.put(newEdge(f, g));
		counter.put(newEdge(g, h));
		counter.put(newEdge(h, i));
		counter.put(newEdge(i, j));
		counter.put(newEdge(b, k));
		counter.put(newEdge(k, l));
		counter.put(newEdge(l, m));
		counter.put(newEdge(m, n));
		counter.put(newEdge(n, o));
		counter.put(newEdge(o, p));
		counter.put(newEdge(p, q));
		counter.put(newEdge(q, i));
		counter.put(newEdge(m, x));
		counter.put(newEdge(x, y));
		counter.put(newEdge(y, z));
		counter.put(newEdge(z, p));
		counter.put(newEdge(l, e));

		System.out.println(counter.getEntries());
		Iterator<Data> it = counter.getEntries().iterator();
		assertEquals("a", it.next().name);
		assertEquals("b", it.next().name);
		assertEquals("k", it.next().name);
		assertEquals("l", it.next().name);
		assertEquals("c", it.next().name);
		assertEquals("d", it.next().name);
		assertEquals("m", it.next().name);
		assertEquals("n", it.next().name);
		assertEquals("o", it.next().name);
		assertEquals("x", it.next().name);
		assertEquals("y", it.next().name);
		assertEquals("z", it.next().name);
		assertEquals("p", it.next().name);
		assertEquals("q", it.next().name);
		assertEquals("e", it.next().name);
		assertEquals("f", it.next().name);
		assertEquals("g", it.next().name);
		assertEquals("h", it.next().name);
		assertEquals("i", it.next().name);
		assertEquals("j", it.next().name);
		assertFalse(it.hasNext());
	}

	public void testSortWithFilter() {
		Data a = new Data("a", 1);
		Data b = new Data("b", 3);
		Data c = new Data("c", 7);
		Data d = new Data("d", 10);
		Data e = new Data("e", 21);
		Data f = new Data("f", 3);
		Data g = new Data("g", 1);
		Data h = new Data("h", 1);
		Data i = new Data("i", 1);
		Data j = new Data("j", 1);
		Data k = new Data("k", 3);
		Data l = new Data("l", 2);
		Data m = new Data("m", 11);
		Data n = new Data("n", 4);
		Data o = new Data("o", 5);
		Data p = new Data("p", 5);
		Data q = new Data("q", 5);
		Data x = new Data("x", 6);
		Data y = new Data("y", 5);
		Data z = new Data("z", 5);

		TopologicalSorter<Data> counter = new TopologicalSorter<Data>(
				new Comparator<Data>() {
					public int compare(Data o1, Data o2) {
						return o1.compareTo(o2);
					}

				}) {
			@Override
			protected boolean filter(Data element) {
				if (element.name.equals("c"))
					return false;
				if (element.name.equals("n"))
					return false;
				return true;
			}

			@Override
			public int size() {
				return super.size() - 2;
			}
		};
		counter.put(newEdge(a, b));
		counter.put(newEdge(b, c));
		counter.put(newEdge(c, d));
		counter.put(newEdge(d, e));
		counter.put(newEdge(e, f));
		counter.put(newEdge(f, g));
		counter.put(newEdge(g, h));
		counter.put(newEdge(h, i));
		counter.put(newEdge(i, j));
		counter.put(newEdge(b, k));
		counter.put(newEdge(k, l));
		counter.put(newEdge(l, m));
		counter.put(newEdge(m, n));
		counter.put(newEdge(n, o));
		counter.put(newEdge(o, p));
		counter.put(newEdge(p, q));
		counter.put(newEdge(q, i));
		counter.put(newEdge(m, x));
		counter.put(newEdge(x, y));
		counter.put(newEdge(y, z));
		counter.put(newEdge(z, p));
		counter.put(newEdge(l, e));

		System.out.println(counter.getEntries());
		Iterator<Data> it = counter.getEntries().iterator();
		assertEquals("a", it.next().name);
		assertEquals("b", it.next().name);
		assertEquals("k", it.next().name);
		assertEquals("l", it.next().name);
		assertEquals("d", it.next().name);
		assertEquals("m", it.next().name);
		assertEquals("o", it.next().name);
		assertEquals("x", it.next().name);
		assertEquals("y", it.next().name);
		assertEquals("z", it.next().name);
		assertEquals("p", it.next().name);
		assertEquals("q", it.next().name);
		assertEquals("e", it.next().name);
		assertEquals("f", it.next().name);
		assertEquals("g", it.next().name);
		assertEquals("h", it.next().name);
		assertEquals("i", it.next().name);
		assertEquals("j", it.next().name);
		assertFalse(it.hasNext());
	}

	/** Test nonlinear access to the array
	 */
	public void testRandomAccess() {
		Data a = new Data("a",1);
		Data b = new Data("b",1);
		Data c = new Data("c",1);

		TopologicalSorter<Data> counter = new TopologicalSorter<Data>() {
			@Override
			protected boolean filter(Data element) {
				return !element.name.equals("b");
			}
			@Override
			public int size() {
				return super.size() - 1;
			}
		};
		counter.put(newEdge(a, b));
		counter.put(newEdge(b, c));
		List<Data> entries = counter.getEntries();
		assertSame(a, entries.get(0));
		assertSame(c, entries.get(1));
		assertSame(c, entries.get(1));
		assertSame(a, entries.get(0));
	}

	public void testError_1() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",1);
		counter.put(newEdge(a,b));
		counter.put(newEdge(b,a));
		try {
			List<Data> entries = counter.getEntries();
			assertSame(a, entries.get(0));
		} catch (IllegalStateException e) {
			return;
		}
		fail("IllegalStateException expected");
	}

	/* FIXME: For cleanliness these cases should pass.
	 * i.e. return as many items as possible and fail
	 * only when we run out of sortable items. For now
	 * we simply know that failure can happen at
	 * any time if and only if a cycle exists
	 *
	public void testError_1() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",1);
		counter.put(newEdge(a,b));
		counter.put(newEdge(b,a));
		List<Data> entries = counter.getEntries();
		try {
			assertSame(a, entries.get(0));
		} catch (IllegalStateException e) {
			return;
		}
		fail("IllegalStateException expected");
	}

	public void testError_2() {
		final TopologicalSorter<Data> counter = newSorter();
		Data a = new Data("a",2);
		Data b = new Data("b",1);
		Data c = new Data("c",1);
		counter.put(newEdge(a,b));
		counter.put(newEdge(b,c));
		counter.put(newEdge(c,b));
		List<Data> entries = counter.getEntries();
		assertSame(a, entries.get(0));
		try {
			assertSame(a, entries.get(1));
		} catch (IllegalStateException e) {
			return;
		}
		fail("IllegalStateException expected");
	}
	*/
}
