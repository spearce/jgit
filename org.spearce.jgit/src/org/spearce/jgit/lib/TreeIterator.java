/*
 *  Copyright (C) 2008 Robin Rosenberg
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */

package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.Iterator;

/**
 * A tree iterator iterates over a tree and all its members recursing into
 * subtrees according to order.
 *
 * Default is to only visit leafs. An {@link Order} value can be supplied to
 * make the iteration include Tree nodes as well either before or after the
 * child nodes have been visited.
 */
public class TreeIterator implements Iterator<TreeEntry> {

	private Tree tree;

	private int index;

	private TreeIterator sub;

	private Order order;

	private boolean visitTreeNodes;

	private boolean hasVisitedTree;

	/**
	 * Traversal order
	 */
	public enum Order {
		/**
		 * Visit node first, then leaves
		 */
		PREORDER,

		/**
		 * Visit leaves first, then node
		 */
		POSTORDER
	};

	/**
	 * Construct a {@link TreeIterator} for visiting all non-tree nodes.
	 *
	 * @param start
	 */
	public TreeIterator(Tree start) {
		this(start, Order.PREORDER, false);
	}

	/**
	 * Construct a {@link TreeIterator} visiting all nodes in a tree in a given
	 * order.
	 *
	 * @param start Root node
	 * @param order {@link Order}
	 */
	public TreeIterator(Tree start, Order order) {
		this(start, order, true);
	}

	/**
	 * Construct a {@link TreeIterator}
	 *
	 * @param start First node to visit
	 * @param order Visitation {@link Order}
	 * @param visitTreeNode True to include tree node
	 */
	private TreeIterator(Tree start, Order order, boolean visitTreeNode) {
		this.tree = start;
		this.visitTreeNodes = visitTreeNode;
		this.index = -1;
		this.order = order;
		if (!visitTreeNodes)
			this.hasVisitedTree = true;
		try {
			step();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public TreeEntry next() {
		try {
			TreeEntry ret = nextTreeEntry();
			step();
			return ret;
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private TreeEntry nextTreeEntry() throws IOException {
		TreeEntry ret;
		if (sub != null)
			ret = sub.nextTreeEntry();
		else {
			if (index < 0 && order == Order.PREORDER) {
				return tree;
			}
			if (order == Order.POSTORDER && index == tree.memberCount()) {
				return tree;
			}
			ret = tree.members()[index];
		}
		return ret;
	}

	public boolean hasNext() {
		try {
			return hasNextTreeEntry();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private boolean hasNextTreeEntry() throws IOException {
		if (tree == null)
			return false;
		return sub != null
			|| index < tree.memberCount()
			|| order == Order.POSTORDER && index == tree.memberCount();
	}

	private boolean step() throws IOException {
		if (tree == null)
			return false;

		if (sub != null) {
			if (sub.step())
				return true;
			sub = null;
		}

		if (index < 0 && !hasVisitedTree && order == Order.PREORDER) {
			hasVisitedTree = true;
			return true;
		}

		while (++index < tree.memberCount()) {
			TreeEntry e = tree.members()[index];
			if (e instanceof Tree) {
				sub = new TreeIterator((Tree) e, order, visitTreeNodes);
				if (sub.hasNextTreeEntry())
					return true;
				sub = null;
				continue;
			}
			return true;
		}

		if (index == tree.memberCount() && !hasVisitedTree
				&& order == Order.POSTORDER) {
			hasVisitedTree = true;
			return true;
		}
		return false;
	}

	public void remove() {
		throw new IllegalStateException(
				"TreeIterator does not suppport remove()");
	}
}
