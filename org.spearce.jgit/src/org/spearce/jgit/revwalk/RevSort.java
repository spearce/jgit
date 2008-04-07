/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.revwalk;

/** Sorting strategies supported by {@link RevWalk}. */
public enum RevSort {
	/**
	 * No specific sorting is requested.
	 * <p>
	 * Applications should not rely upon the ordering produced by this strategy.
	 * Any ordering in the output is caused by low level implementation details
	 * and may change without notice.
	 */
	NONE,

	/**
	 * Sort by commit time, descending (newest first, oldest last).
	 * <p>
	 * This strategy can be combined with {@link #TOPO}.
	 */
	COMMIT_TIME_DESC,

	/**
	 * Topological sorting (all children before parents).
	 * <p>
	 * This strategy can be combined with {@link #COMMIT_TIME_DESC}.
	 */
	TOPO,

	/**
	 * Flip the output into the reverse ordering.
	 * <p>
	 * This strategy can be combined with the others described by this type as
	 * it is usually performed at the very end.
	 */
	REVERSE,

	/**
	 * Include {@link RevFlag#UNINTERESTING} boundary commits after all others.
	 * <p>
	 * A boundary commit is a UNINTERESTING parent of an interesting commit that
	 * was previously output.
	 */
	BOUNDARY;
}
