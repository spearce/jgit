/*
 *  Copyright (C) 2008  Roger C. Soares
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
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.ui.internal.history;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Results for the find toolbar. This object stores the rows in the history
 * table that contain a match to a given pattern.
 *
 * @see FindToolbar
 * @see FindToolbarThread
 */
public class FindResults {
	private Map<Integer, Integer> matchesMap = new LinkedHashMap<Integer, Integer>();

	Integer[] keysArray;

	private int matchesCount;

	/**
	 * Returns if the index in the history table matches the find pattern.
	 *
	 * @param index
	 *            history table item index.
	 * @return boolean <code>true</code> if the history table
	 *         <code>index</code> contains a match to the find pattern,
	 *         <code>false</code> otherwise
	 */
	public synchronized boolean isFoundAt(int index) {
		return matchesMap.containsKey(Integer.valueOf(index));
	}

	/**
	 * Returns the first table item index after <code>index</code> that
	 * contains a match to the find pattern.
	 *
	 * @param index
	 *            the history table item index
	 * @return the index after <code>index</code> that contains a match.
	 *         Returns -1 if there isn't a match after <code>index</code>
	 */
	public synchronized int getIndexAfter(int index) {
		Integer[] matches = getkeysArray();
		int sres = Arrays.binarySearch(matches, Integer.valueOf(index));
		if (sres >= 0 && sres != matches.length - 1) {
			return matches[sres + 1].intValue();
		} else if (sres < 0) {
			sres = -sres - 1;
			if (sres < matches.length) {
				return matches[sres].intValue();
			}
		}

		return -1;
	}

	/**
	 * Returns the first table item index before <code>index</code> that
	 * contains a match to the find pattern.
	 *
	 * @param index
	 *            the history table item index
	 * @return the index before <code>index</code> that contains a match.
	 *         Returns -1 if there isn't a match before <code>index</code>
	 */
	public synchronized int getIndexBefore(int index) {
		Integer[] matches = getkeysArray();
		int sres = Arrays.binarySearch(matches, Integer.valueOf(index));
		if (sres >= 0 && sres != 0) {
			return matches[sres - 1].intValue();
		} else if (sres < -1) {
			sres = -sres;
			return matches[sres - 2].intValue();
		}

		return -1;
	}

	/**
	 * Returns the first table item index that contains a match to the find
	 * pattern.
	 *
	 * @return the first index that contains a match. Returns -1 if there isn't
	 *         any match
	 */
	public synchronized int getFirstIndex() {
		Iterator iter = matchesMap.keySet().iterator();
		if (iter.hasNext()) {
			return ((Integer) iter.next()).intValue();
		}

		return -1;
	}

	/**
	 * Returns the last table item index that contains a match to the find
	 * pattern.
	 *
	 * @return the last index that contains a match. Returns -1 if there isn't
	 *         any match
	 */
	public synchronized int getLastIndex() {
		Integer[] matches = getkeysArray();
		if (matches.length > 0) {
			return matches[matches.length - 1].intValue();
		}

		return -1;
	}

	/**
	 * Returns the index in the matches list for the history table item
	 * <code>index</code>.
	 *
	 * @param index
	 *            the history table item index
	 * @return the position of the <code>index</code> in the total matches
	 *         list. Returns -1 if <code>index</code> doesn't contain a match
	 */
	public synchronized int getMatchNumberFor(int index) {
		Integer ix = matchesMap.get(Integer.valueOf(index));
		if (ix != null) {
			return ix.intValue();
		}

		return -1;
	}

	/**
	 * @return int
	 */
	public int size() {
		return matchesCount;
	}

	/**
	 * Cleans the find results. All match item indexes are removed.
	 */
	public synchronized void clear() {
		matchesMap.clear();
		keysArray = null;
		matchesCount = 0;
	}

	/**
	 * Adds a history table item index (<code>matchIx</code>) to the find
	 * results matches list.
	 *
	 * @param matchIx
	 *            the history table item index that matches a find pattern.
	 */
	public synchronized void add(int matchIx) {
		matchesMap.put(Integer.valueOf(matchIx), Integer
				.valueOf(++matchesCount));
		keysArray = null;
	}

	private Integer[] getkeysArray() {
		if (keysArray == null) {
			keysArray = matchesMap.keySet().toArray(
					new Integer[matchesMap.size()]);
		}

		return keysArray;
	}

}
