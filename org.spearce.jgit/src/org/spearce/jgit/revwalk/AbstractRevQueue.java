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

abstract class AbstractRevQueue extends Generator {
	/** Current output flags set for this generator instance. */
	int outputType;

	/**
	 * Add a commit if it does not have a flag set yet, then set the flag.
	 * <p>
	 * This method permits the application to test if the commit has the given
	 * flag; if it does not already have the flag than the commit is added to
	 * the queue and the flag is set. This later will prevent the commit from
	 * being added twice.
	 * 
	 * @param c
	 *            commit to add.
	 * @param queueControl
	 *            flag that controls admission to the queue.
	 */
	public final void add(final RevCommit c, final RevFlag queueControl) {
		if (!c.has(queueControl)) {
			c.add(queueControl);
			add(c);
		}
	}

	/**
	 * Add a commit's parents if one does not have a flag set yet.
	 * <p>
	 * This method permits the application to test if the commit has the given
	 * flag; if it does not already have the flag than the commit is added to
	 * the queue and the flag is set. This later will prevent the commit from
	 * being added twice.
	 * 
	 * @param c
	 *            commit whose parents should be added.
	 * @param queueControl
	 *            flag that controls admission to the queue.
	 */
	public final void addParents(final RevCommit c, final RevFlag queueControl) {
		final RevCommit[] pList = c.parents;
		if (pList == null)
			return;
		for (RevCommit p : pList)
			add(p, queueControl);
	}

	/**
	 * Remove the first commit from the queue.
	 * 
	 * @return the first commit of this queue.
	 */
	public abstract RevCommit next();

	/** Remove all entries from this queue. */
	public abstract void clear();

	abstract boolean everbodyHasFlag(int f);

	abstract boolean anybodyHasFlag(int f);

	@Override
	int outputType() {
		return outputType;
	}
}
