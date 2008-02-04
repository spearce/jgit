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

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.spearce.jgit.lib.TopologicalSorter.Edge;
import org.spearce.jgit.lib.TopologicalSorter.Lane;

public class LaneTest extends TestCase {

	TopologicalSorter<Data> counter = new TopologicalSorter<Data>(
			new Comparator<Data>() {
				public int compare(Data o1, Data o2) {
					return o1.compareTo(o2);
				}
			});

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

	public void testLayout() {
		/*
		 * This is th test input. A list of nodes. The number is a weight that
		 * is used when the output order cannot be determined conclusively from
		 * the order of the nodes. Think of the number as a date in a git
		 * commit. The elements are sorted by weight (the number in parentheses)
		 * when the topological order is non defined.
		 *
		 * a(1) - b(3) - c(7) - d(10) - e(21) - f(3) - g(1) - h(1) - i(1) - j(1) -
		 *         \                 /                                /
		 *          k(3)    -    l(2) - m(11) - n(4) - o(5) - p(5) - q(5)
		 *                                \                /
		 *                                 x(6) - y(5) - z(f)
		 *
		 *	a
		 *	|
		 *  b---·
		 *  |	|
		 *	|   k
		 *	|	|
		 *  |	l---·
		 *  |	|	|
		 *  c   | 	|
		 *  |   |	|
		 *  d   |	|
		 *  |   |	|
		 *  |	m---+---·
		 *  |	|	|	|
		 *  |	n	|	|
		 *  |	|	|	|
		 *  |	o	|	|
		 *  |	|	|	|
		 *  x<--+---+---·
		 *  |	|	|
		 *  y	|	|
		 *  |	|	|
		 *  z	|	|
		 *  |	|	|
		 *  |-->p	|
		 *  |	|	|
		 *  |	q	|
		 *  |	|	|
		 *  e<--+---·
		 *  |	|
		 *  f	|
		 *  |	|
		 *  g	|
		 *  |	|
		 *  h	|
		 *  |	|
		 *  i<--|
		 *  |
		 *  j
		 *
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

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(b, c));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		counter.put(new TopologicalSorter.Edge<Data>(d, e));
		counter.put(new TopologicalSorter.Edge<Data>(e, f));
		counter.put(new TopologicalSorter.Edge<Data>(f, g));
		counter.put(new TopologicalSorter.Edge<Data>(g, h));
		counter.put(new TopologicalSorter.Edge<Data>(h, i));
		counter.put(new TopologicalSorter.Edge<Data>(i, j));
		counter.put(new TopologicalSorter.Edge<Data>(b, k));
		counter.put(new TopologicalSorter.Edge<Data>(k, l));
		counter.put(new TopologicalSorter.Edge<Data>(l, m));
		counter.put(new TopologicalSorter.Edge<Data>(m, n));
		counter.put(new TopologicalSorter.Edge<Data>(n, o));
		counter.put(new TopologicalSorter.Edge<Data>(o, p));
		counter.put(new TopologicalSorter.Edge<Data>(p, q));
		counter.put(new TopologicalSorter.Edge<Data>(q, i));
		counter.put(new TopologicalSorter.Edge<Data>(m, x));
		counter.put(new TopologicalSorter.Edge<Data>(x, y));
		counter.put(new TopologicalSorter.Edge<Data>(y, z));
		counter.put(new TopologicalSorter.Edge<Data>(z, p));
		counter.put(new TopologicalSorter.Edge<Data>(l, e));

		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);

		assertEquals(road, road2);
		assertEquals(
				"a\n" +
				"b>>>>>>>\\\n" +
				"|       k\n" +
				"|       l>>>>>>>\\\n" +
				"c       |       |\n" +
				"d>>>>>>>|>>>>>>>\\\n" +
				"        m>>>>>>>|>>>>>>>\\\n" +
				"        n       |       |\n" +
				"        o       |       |\n" +
				"        |       |       x\n" +
				"        |       |       y\n" +
				"        /<<<<<<<|<<<<<<<z\n" +
				"        p       |\n" +
				"        q       |\n" +
				"        |       e\n" +
				"        |       f\n" +
				"        |       g\n" +
				"        /<<<<<<<h\n" +
				"        i\n" +
				"        j\n",
				road);

		for(Data xx : counter.getEntries()) {
			Lane lj = counter.lane.get(xx);
			System.out.println(":"+xx + ", lane "+ lj + "{" + (lj.startsAt!=null ? counter.lane.get(lj.startsAt).getNumber() : -1) + "," + counter.lane.get(lj.endsAt).getNumber()+"}");
		}
		for (TopologicalSorter<Data>.Lane li : counter.lane.values()) {
			System.out.println("L "+li);
		}

		Data da;
		Lane la;
		Iterator<Data> di = counter.getEntries().iterator();
		da=di.next(); la=counter.lane.get(da); assertEquals("a",da.name); assertEquals(0, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("b",da.name); assertEquals(0, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("k",da.name); assertEquals(1, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("l",da.name); assertEquals(1, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("c",da.name); assertEquals(0, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("d",da.name); assertEquals(0, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("m",da.name); assertEquals(1, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("n",da.name); assertEquals(1, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("o",da.name); assertEquals(1, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("x",da.name); assertEquals(3, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("y",da.name); assertEquals(3, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("z",da.name); assertEquals(3, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("p",da.name); assertEquals(1, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("q",da.name); assertEquals(1, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("e",da.name); assertEquals(2, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("f",da.name); assertEquals(2, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("g",da.name); assertEquals(2, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("h",da.name); assertEquals(2, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("i",da.name); assertEquals(1, la.getNumber());
		da=di.next(); la=counter.lane.get(da); assertEquals("j",da.name); assertEquals(1, la.getNumber());
		assertFalse(di.hasNext());
	}

	public void testZeroNodes() {
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"",
				road);
	}

	public void testOneNode() {
		Data a = new Data("a", 5);

		counter.put(a);
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"a\n",
				road);
	}

	public void testTwoNodes() {
		Data a = new Data("a", 5);
		Data b = new Data("b", 4);

		counter.put(a);
		counter.put(b);
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"b\n" +
				"        a\n",
				road);
	}

	public void testOneInTwoOut() {
		Data a = new Data("a", 5);
		Data b = new Data("b", 4);
		Data c = new Data("c", 3);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(a, c));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"a>>>>>>>\\\n"+
				"|       c\n" +
				"b\n"
				, road);
	}

	public void testTwoInOneOut() {
		Data a = new Data("a", 5);
		Data b = new Data("b", 4);
		Data c = new Data("c", 3);

		counter.put(new TopologicalSorter.Edge<Data>(b, a));
		counter.put(new TopologicalSorter.Edge<Data>(c, a));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"c\n"+
				"/<<<<<<<b\n" +
				"a\n"
				, road);
	}

	public void testTwoLanesWithFourNode_variant1() {
		Data a = new Data("a", 5);
		Data b = new Data("b", 4);
		Data c = new Data("c", 3);
		Data d = new Data("d", 2);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"c\n"+
				"d\n"+
				"        a\n"+
				"        b\n"
				, road);
	}

	public void testTwoLanesWithFourNode_variant2() {
		Data a = new Data("a", 3);
		Data b = new Data("b", 2);
		Data c = new Data("c", 5);
		Data d = new Data("d", 4);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"a\n"+
				"b\n"+
				"        c\n"+
				"        d\n"
				, road);
	}

	public void testTwoLanesWithFourNode_variant3() {
		Data a = new Data("a", 3);
		Data b = new Data("b", 4);
		Data c = new Data("c", 2);
		Data d = new Data("d", 5);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"c\n"+
				"|       a\n"+
				"|       b\n"+
				"d\n"
				, road);
	}

	public void testTwoLanesWithFourNode_variant4() {
		Data a = new Data("a", 3);
		Data b = new Data("b", 5);
		Data c = new Data("c", 2);
		Data d = new Data("d", 4);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"c\n"+
				"|       a\n"+
				"d       |\n"+
				"        b\n"
				, road);
	}

	public void testDiamond() {
		Data a = new Data("a", 3);
		Data b = new Data("b", 4);
		Data c = new Data("c", 2);
		Data d = new Data("d", 5);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(a, c));
		counter.put(new TopologicalSorter.Edge<Data>(b, d));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"a>>>>>>>\\\n"+
				"|       c\n"+
				"b>>>>>>>\\\n"+
				"        d\n"
				, road);
	}

	public void testAllDirections() {
		Data a = new Data("a",1);
		Data b = new Data("b",2);
		Data c = new Data("c",5);
		Data d = new Data("d",4);
		Data e = new Data("e",3);
		Data f = new Data("f",6);
		Data g = new Data("g",7);
		counter.put(new TopologicalSorter.Edge<Data>(a,b));
		counter.put(new TopologicalSorter.Edge<Data>(b,c));
		counter.put(new TopologicalSorter.Edge<Data>(a,d));
		counter.put(new TopologicalSorter.Edge<Data>(b,d));
		counter.put(new TopologicalSorter.Edge<Data>(d,c));
		counter.put(new TopologicalSorter.Edge<Data>(e,d));
		counter.put(new TopologicalSorter.Edge<Data>(e,g));
		counter.put(new TopologicalSorter.Edge<Data>(d,g));
		counter.put(new TopologicalSorter.Edge<Data>(d,f));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
"a>>>>>>>\\\n"+
"b>>>>>>>\\\n"+
"|       /<<<<<<<e\n"+
"/<<<<<<<d>>>>>>>\\\n"+
"c       |       |\n"+
"        f       |\n"+
"                g\n"
				, road);
	}

	public void testAllPossibleWithFour() {
		Data a = new Data("a", 1);
		Data b = new Data("b", 2);
		Data c = new Data("c", 3);
		Data d = new Data("d", 4);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(a, c));
		counter.put(new TopologicalSorter.Edge<Data>(a, d));
		counter.put(new TopologicalSorter.Edge<Data>(b, c));
		counter.put(new TopologicalSorter.Edge<Data>(b, d));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"a>>>>>>>\\>>>>>>>\\\n"+
				"b>>>>>>>\\>>>>>>>\\\n"+
				"        c>>>>>>>\\\n"+
				"                d\n"
				, road);
	}

	public void testAllPossibleWithFive() {
		Data a = new Data("a", 1);
		Data b = new Data("b", 2);
		Data c = new Data("c", 3);
		Data d = new Data("d", 4);
		Data e = new Data("e", 5);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(a, c));
		counter.put(new TopologicalSorter.Edge<Data>(a, d));
		counter.put(new TopologicalSorter.Edge<Data>(a, e));
		counter.put(new TopologicalSorter.Edge<Data>(b, c));
		counter.put(new TopologicalSorter.Edge<Data>(b, d));
		counter.put(new TopologicalSorter.Edge<Data>(b, e));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		counter.put(new TopologicalSorter.Edge<Data>(c, e));
		counter.put(new TopologicalSorter.Edge<Data>(d, e));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"a>>>>>>>\\>>>>>>>\\>>>>>>>\\\n"+
				"b>>>>>>>\\>>>>>>>\\>>>>>>>\\\n"+
				"        c>>>>>>>\\>>>>>>>\\\n"+
				"                d>>>>>>>\\\n"+
				"                        e\n"
				, road);
	}

	public void testAllPossibleWithFiveLessSome() {
		Data a = new Data("a", 1);
		Data b = new Data("b", 2);
		Data c = new Data("c", 3);
		Data d = new Data("d", 4);
		Data e = new Data("e", 5);

		counter.put(new TopologicalSorter.Edge<Data>(a, b));
		counter.put(new TopologicalSorter.Edge<Data>(a, c));
		counter.put(new TopologicalSorter.Edge<Data>(a, d));
		counter.put(new TopologicalSorter.Edge<Data>(a, e));
		counter.put(new TopologicalSorter.Edge<Data>(b, c));
		counter.put(new TopologicalSorter.Edge<Data>(b, e));
		counter.put(new TopologicalSorter.Edge<Data>(c, d));
		counter.put(new TopologicalSorter.Edge<Data>(d, e));
		String road = drawAsAscii(counter);
		String road2 = drawAsAscii(counter);
		assertEquals(road,road2);
		assertEquals(
				"a>>>>>>>\\>>>>>>>\\>>>>>>>\\\n"+
				"b>>>>>>>\\>>>>>>>|>>>>>>>\\\n"+
				"        c>>>>>>>\\       |\n"+
				"                d>>>>>>>\\\n"+
				"                        e\n"
				, road);
	}

	@SuppressWarnings("boxing")
	private String drawAsAscii(TopologicalSorter<Data> counter) {
		StringWriter w = new StringWriter();
		List<Data> entries = counter.getEntries();
		for (int i=0; i<entries.size(); ++i) {
			Data xx = entries.get(i);
			Integer io = counter.getInternalPosition(xx);
//			System.out.print("Now at item "+xx+ " at "+io);
			Lane lane = counter.lane.get(xx);
//			lane.getNumber();
			char[] points = new char[counter.currentLanes.size()];
			char[] horiz = new char[counter.currentLanes.size()-1];
			Arrays.fill(points,' ');

			List<Edge<Data>> px = counter.getEdgeFrom(xx);
			for (TopologicalSorter<Data>.Lane li : counter.currentLanes) {
				Integer iost = counter.getInternalPosition(li.startsAt);
				Integer ioen = counter.getInternalPosition(li.endsAt);

				// Middle of lane
				//	 o
				//	 |
				//	 o
				if (iost != null && io > iost && (ioen == null || io < ioen)) {
					points[li.getNumber()] = '|';
				}
				// branch out to parent
				//   o						 o
				//   \>>>>>>>>>>o o<<<<<<<<<</
				if (px != null) {
					for (int j=0; j<px.size(); ++j) {
						Data p = px.get(j).to;
						Lane pl = counter.getLane(p);
						if (li == pl && lane != li) {
							int fromn = lane.getNumber();
							int ton = pl.getNumber();
							if (fromn < ton) {
								points[ton] = '\\';
								for (int k=fromn; k<ton; ++k) {
									horiz[k] = '>';
								}
							} else {
								points[ton] = '/';
								for (int k=fromn-1; k>=ton; --k) {
									horiz[k] = '<';
								}
							}
						}
					}
				}
				// merge, SAME (unless we get really smart)
			}
			points[lane.getNumber()] = xx.name.charAt(0);
			int m = points.length;
			while (m > 0 && points[m-1] == ' ')
				--m;
			for (int ii=0; ii<m-1; ++ii) {
				w.write(points[ii]);
				char ch = horiz[ii];
				if (ch != 0)
					for (int jj=0; jj<7; ++jj)
						w.write(ch);
				else
					w.write("       ");
//					w.write('\t');
			}
			w.write(points[m-1]);
			w.write('\n');
		}
		System.out.println(w);
		return w.toString();
	}
}
