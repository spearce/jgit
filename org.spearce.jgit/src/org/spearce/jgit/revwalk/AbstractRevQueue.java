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

abstract class AbstractRevQueue {
	/**
	 * Insert the commit pointer at the end of the queue.
	 * 
	 * @param c
	 *            the commit to insert into the queue.
	 */
	public abstract void add(final RevCommit c);

	/**
	 * Remove the first commit from the queue.
	 * 
	 * @return the first commit of this queue.
	 */
	public abstract RevCommit pop();

	/** Remove all entries from this queue. */
	public abstract void clear();

	abstract boolean everbodyHasFlag(int f);
}
