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

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A topological sorter. Use it by adding elemens and/or edges. Then the nodes
 * can be retrieved in sequence by either invoking the iterator() or calling
 * getEntries(). Both are lazy methods that do not wait for the sort to complete
 * before returning data. The sort is executed incrementally.
 *
 * This object is not reusable and nodes cannot be added after sorting has
 * commenced.
 *
 * The methods size() and filter() can be overridden to include only a subset of
 * the elements in the set.
 *
 * @author Robin Rosenberg
 *
 * @param <T>
 *            The node type in the graph
 */
public class TopologicalSorter<T> {

	private Comparator<T> comparator;

	private Set<T> allNodes = new HashSet<T>();

	private Map<T, List<Edge<T>>> allEdges = new TreeMap<T, List<Edge<T>>>();

	private Map<T, Integer> inCount = new HashMap<T, Integer>();

	private Collection<T> zeroIn; // init later, when we know which comparator to use

	private List<T> entries;

	private Map<T, Integer> internalOrder = new HashMap<T, Integer>();

	public List<Lane> currentLanes = new ArrayList<Lane>();

	int lastLane = 0;

	Map<T, Lane> lane = new HashMap<T, Lane>();

	/**
	 * Construct a topological sorter.
	 *
	 * @param comparator
	 *            A comparator that orders nodes that have no defined order in
	 *            the graph. For those element for which the graph conclusively
	 *            defined the order the result has no relevance.
	 */
	public TopologicalSorter(final Comparator<T> comparator) {
		this.comparator = comparator;
	}

	/**
	 * Construct a topological sorter. You will need to invoke setComparator to
	 * complete construction.
	 */
	public TopologicalSorter() {
		// empty
	}

	/**
	 * Set the comparator to use. Use this method if the constructor cannot be
	 * used.
	 *
	 * @param comparator
	 *            See constructor.
	 */
	public void setComparator(final Comparator<T> comparator) {
		this.comparator = comparator;
	}

	/** Defines a directed edge
	 *
	 * @param <T>
	 *            Node type
	 */
	public static class Edge<T> {
		final T from;

		final T to;

		/**
		 * Construct an edge from a node to another,
		 *
		 * @param from
		 * @param to
		 */
		public Edge(T from, T to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public String toString() {
			return from + " -> " + to;
		}

		@Override
		public int hashCode() {
			return from.hashCode() | to.hashCode();
		}

		/**
		 * @return the origin of this edge
		 */
		public T getTo() {
			return to;
		}

		/**
		 * @return the destination of this edge
		 */
		public T getFrom() {
			return from;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			Edge<T> o = (Edge<T>) obj;
			return from == o.from && to == o.to
					|| from.equals(((Edge<T>) obj).from)
					&& to.equals(((Edge<T>) obj).to);
		}
	}

	/**
	 * Copy a sorter. This must be invoked <em>before</em> iteration starts.
	 * Mostly for testing.
	 *
	 * @param o Another sorter whose raw state we will copy
	 */
	public TopologicalSorter(TopologicalSorter<T> o) {
		if (zeroIn != null)
			throw new IllegalStateException("Cannot clone after iteration starts");
		comparator = o.comparator;
		allNodes.addAll(o.allNodes);
		allEdges.putAll(o.allEdges);
		inCount.putAll(o.inCount);
	}

	/**
	 * Add an edge to the graph
	 *
	 * @param edge
	 */
	public void put(Edge<T> edge) {
//		System.out.println("put("+edge+")");
		assert !edge.from.equals(edge.to);
		List<Edge<T>> edges = allEdges.get(edge.from);
		// optimize for very small arrays, one or two parents are overwhelmingly
		// the most common ones, the rest can be counted on one or two hands in
		// most repos. Not even the git repo has more than ~40 merges with more than
		// two parents.
		if (edges == null) {
			edges = Collections.singletonList(edge);
			allEdges.put(edge.from, edges);
		} else {
			if (edges.contains(edge)) {
				System.out.println("duplicate edge "+edge+" added");
				return; // in case the frontend feeds duplicates we ignore them
			}
			if (edges.size() == 1) {
				List<Edge<T>> old = edges;
				edges = new ArrayList<Edge<T>>(2);
				edges.add(old.get(0));
			}
			edges.add(edge);
			allEdges.put(edge.from, edges);
		}
		allNodes.add(edge.from);
		allNodes.add(edge.to);
		Integer n = inCount.get(edge.to);
		if (n == null)
			inCount.put(edge.to, new Integer(1));
		else
			inCount.put(edge.to, new Integer(n.intValue() + 1));
	}

	/**
	 * @param from A node
	 * @return All edges from the node 'from'. may be null
	 */
	public List<Edge<T>> getEdgeFrom(T from) {
		return allEdges.get(from);
	}

	/**
	 * Add a single node to the graph.
	 *
	 * @param node
	 */
	public void put(T node) {
		// System.out.println("put("+node+")");
		allNodes.add(node);
	}

	private void removeallfrom(T from) {
		Collection<Edge<T>> fromEdges = allEdges.get(from);
		if (fromEdges != null) {
			for (Iterator<Edge<T>> i = fromEdges.iterator(); i.hasNext();) {
				Edge<T> e = i.next();
				Lane l = lane.get(e.to);
				if (l == null) {
					for (Lane m : currentLanes) {
						if (m.endsAt == from) {
							l = m;
							break;
						}
					}
					if (l == null) {
						l = newLane();
						l.startsAt = e.from;
					}
					l.endsAt = e.to;
					lane.put(e.to, l);
				} else {
					l.endsAt = e.to;
				}
				Integer c = inCount.get(e.to);
				if (c.intValue() == 1) {
					zeroIn.add(e.to);
					inCount.remove(e.to);
				} else {
					inCount.put(e.to, new Integer(c.intValue() - 1));
				}
			}
//			allEdges.remove(from);
			int n = inCount.size();
			if (n % 10000 == 0)
				System.out.println("node left:" + n);
		}
	}

	public Lane getLane(T node) {
		return lane.get(node);
	}

	Lane newLane() {
		Lane ret = new Lane();
		currentLanes.add(ret);
		return ret;
	}

	public class Lane {
		public T endsAt;

		public T startsAt;

		private int number = -1;

		Lane() {
		}

		@Override
		public boolean equals(Object obj) {
			return number == ((Lane) obj).number;
		}

		@Override
		public int hashCode() {
			return number;
		}

		@Override
		public String toString() {
			return "L[" + number + "]("+startsAt+" to "+endsAt+")";
		}

		public List<Lane> getAllLanes() {
			return currentLanes;
		}

		public int getNumber() {
			if (number == -1)
				number = lastLane++;
			return number;
		}

		public TopologicalSorter<T> getSorter() {
			return TopologicalSorter.this;
		}
	}

	T nextZero() {
		if (zeroIn == null) {
			zeroIn = new TreeSet<T>(comparator);
			for (T i : allNodes) {
				if (inCount.get(i) == null) {
					zeroIn.add(i);
					Lane l = newLane();
					lane.put(i, l);
					l.startsAt = i;
					l.endsAt = i;
				}
			}
		}
		T ret;
		if (zeroIn.isEmpty()) {
			if (inCount.size() > 0)
				throw new IllegalStateException("Topological sort failed, "
						+ inCount.size() + " nodes left");
			return null;
		}
		Iterator<T> i = zeroIn.iterator();
		ret = i.next();
		internalOrder.put(ret, new Integer(internalOrder.size()));
		i.remove();
		removeallfrom(ret);
		return ret;
	}

	private class TopologicalIterator implements Iterator<T> {

		private T nextValue;

		TopologicalIterator() {
			nextValue = nextZero();
		}

		public boolean hasNext() {
			return nextValue != null;
		}

		public T next() {
			T ret = nextValue;
			nextValue = nextZero();
			return ret;
		}

		public void remove() {
			throw new IllegalStateException("Cannot remove element from "
					+ getClass().getName());
		}

		public TopologicalSorter<T> getSorter() {
			return TopologicalSorter.this;
		}
	}

	/**
	 * @param element
	 *            is the candidate element from the input set
	 * @return true if the element should be included in the final result
	 */
	@SuppressWarnings("unused")
	protected boolean filter(T element) {
		return true;
	}

	/**
	 * Compute the number of elements in the filtered output. This number is
	 * typically known before the order of elements is known.
	 *
	 * @return number of elements in the result set, after filtering.
	 */
	public int size() {
		return allNodes.size();
	}

	/**
	 * Get all sorted nodes as a list. The list is lazy, so nodes at the
	 * beginning may be retrieved before the full sort is completed.
	 *
	 * @return The sorted list of nodes
	 */
	public List<T> getEntries() {
		if (entries == null) {
			entries = new AbstractList<T>() {
				private Object[] elements = new Object[TopologicalSorter.this.size()];

				private int fillCount;

				private Iterator<T> filler = new TopologicalIterator();

				public boolean add(T o) {
					throw new UnsupportedOperationException();
				}

				public void add(int index, T element) {
					throw new UnsupportedOperationException();
				}

				public boolean addAll(Collection<? extends T> c) {
					throw new UnsupportedOperationException();
				}

				public boolean addAll(int index, Collection<? extends T> c) {
					throw new UnsupportedOperationException();
				}

				public void clear() {
					throw new UnsupportedOperationException();
				}

				public boolean contains(Object o) {
					return super.contains(o);
				}

				public boolean containsAll(Collection<?> c) {
					return super.containsAll(c);
				}

				@SuppressWarnings("unchecked")
				public T get(int index) {
					fill(index + 1);
					return (T) elements[index];
				}

				public Iterator<T> iterator() {
					return super.iterator();
				}

				public int lastIndexOf(Object o) {
					return super.lastIndexOf(o);
				}

				public boolean remove(Object o) {
					throw new UnsupportedOperationException();
				}

				public T remove(int index) {
					throw new UnsupportedOperationException();
				}

				public boolean removeAll(Collection<?> c) {
					throw new UnsupportedOperationException();
				}

				public boolean retainAll(Collection<?> c) {
					throw new UnsupportedOperationException();
				}

				public T set(int index, T element) {
					throw new UnsupportedOperationException();
				}

				public int size() {
					return elements.length;
				}

				public Object[] toArray() {
					return toArray(new Object[size()]);
				}

				@SuppressWarnings("unchecked")
				public <AT> AT[] toArray(AT[] a) {
					fill(size());
					if (a.length != size())
						a = (AT[]) Array.newInstance(a.getClass()
								.getComponentType(), size());
					System.arraycopy(elements, 0, a, 0, a.length);
					return a;
				}

				private void fill(int tosize) {
					while (fillCount < tosize) {
						if (!filler.hasNext())
							throw new IllegalStateException("Wrong size or filter");
						T e = filler.next();
						if (filter(e)) {
							elements[fillCount++] = e;
						}
					}
				}
			};
		}
		return entries;
	}

	/**
	 * Get the internal order, before filtering, of this node. Since the algorithm is
	 * incremental a null can be returned to indicate the sorted position is not yet
	 * known.
	 *
	 * @param node
	 * @return The position of a particular node
	 */
	public Integer getInternalPosition(T node) {
		return internalOrder.get(node);
	}
}
