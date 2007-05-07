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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Very much like a map, but specialized
 *  to partition the data on the first byte
 *  of the key. This is MUCH faster. See test
 *  class for how these numbers were derived.
 *  
 *	TreeMap=            2968
 *	HashMap=            1689
 *	Partitioned TreeMap=1499
 *	Partitioned HashMap=1782
 *
 *  Inspiration from Git pack file format which uses this technique.
 *  
 */
public class ObjectIdMap implements Map {

	Map[] level0 = new Map[256];
	
	public ObjectIdMap() {
		this(new TreeMap());
	}

	public ObjectIdMap(Map sample) {
		try {
			Method m=sample.getClass().getMethod("clone", null);
			for (int i=0; i<256; ++i) {
				level0[i] = (Map)m.invoke(sample, null);
			}
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public void clear() {
		for (int i=0; i<256; ++i)
			level0[i].clear();
	}

	public boolean containsKey(Object key) {
		return submap(key).containsKey(key);
	}

	private final Map submap(Object key) {
		return level0[((ObjectId)key).getFirstByte()];
	}

	public boolean containsValue(Object value) {
		for (int i=0; i<256; ++i)
			if (level0[i].containsValue(value))
				return true;
		return false;
	}

	public Set entrySet() {
		Set ret = new HashSet();
		for (int i=0; i<256; ++i)
			ret.addAll(level0[i].entrySet());
		return ret;
	}

	public Object get(Object key) {
		return submap(key).get(key);
	}

	public boolean isEmpty() {
		for (int i=0; i<256; ++i)
			if (!level0[i].isEmpty())
				return false;
		return true;
	}

	public Set keySet() {
		Set ret = new HashSet();
		for (int i=0; i<256; ++i)
			ret.addAll(level0[i].keySet());
		return ret;
	}

	public Object put(Object key, Object value) {
		return submap(key).put(key, value);
	}

	public void putAll(Map arg0) {
		for (Iterator i=arg0.keySet().iterator(); i.hasNext(); ) {
			Object k=i.next();
			Object v=arg0.get(k);
			put(k,v);
		}
	}

	public Object remove(Object key) {
		return submap(key).remove(key);
	}

	public int size() {
		int ret=0;
		for (int i=0; i<256; ++i)
			ret += level0[i].size();
		return ret;
	}

	public Collection values() {
		List ret=new ArrayList(size());
		for (int i=0; i<256; ++i)
			ret.addAll(level0[i].values());
		return ret;
	}

}
