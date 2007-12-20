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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A SuperList is a list composed of other Lists.
 * The content of the sublists is not copied.
 *
 * @param <T> Element type
 */
public class SuperList<T> extends AbstractList<T> {

	List<List<T>> subLists = new ArrayList<List<T>>();
	List<Integer> subListEnd = new ArrayList<Integer>();

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(Collection<? extends T> c) {
		if (c instanceof List)
			return addAll((List<T>)c);
		throw new UnsupportedOperationException();
	}

	/**
	 * Add a sublist to this list.
	 * @param subList
	 * @return true.
	 */
	public boolean addAll(List<T> subList) {
		int lastEnd = subLists.size() > 0 ? subListEnd.get(subListEnd.size()-1).intValue() : 0;
		subListEnd.add(new Integer(lastEnd + subList.size()));
		subLists.add(subList);
		return true;
	}

	@Override
	public boolean add(T o) {
		return addAll(Collections.singletonList(o));
	}

	@Override
	public T get(int index) {
		for (int i = 0; i<subLists.size(); ++i) {
			List<T> l = subLists.get(i);
			int end = subListEnd.get(i).intValue();
			int start  = end - l.size();
			if (index >= start && index < end) {
				int nindex = index - (end - l.size());
				return l.get(nindex);
			}
		}
		throw new ArrayIndexOutOfBoundsException(index);
	}

	@Override
	public int size() {
		if (subListEnd.size() == 0)
			return 0;
		return subListEnd.get(subListEnd.size()-1).intValue();
	}
}
