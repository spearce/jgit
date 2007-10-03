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
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Very much like a map, but specialized to partition the data on the first byte
 * of the key. This is about twice as fast. See test class.
 *
 * Inspiration is taken from the Git pack file format which uses this technique
 * to improve lookup performance.
 *
 * @param <V>
 *            The value we map ObjectId's to.
 *
 */
public class ObjectIdMap<V> extends AbstractMap<ObjectId, V> {

	@SuppressWarnings("unchecked")
	final Map<ObjectId, V>[] level0 = new Map[256];
	
	/**
	 * Construct an ObjectIdMap with an underlying TreeMap implementation
	 */
	public ObjectIdMap() {
		this(new TreeMap());
	}

	/**
	 * Construct an ObjectIdMap with the same underlying map implementation as
	 * the provided example.
	 *
	 * @param sample
	 */
	@SuppressWarnings("unchecked")
	public ObjectIdMap(Map sample) {
		try {
			Method m=sample.getClass().getMethod("clone", (Class[])null);
			for (int i=0; i<256; ++i) {
				level0[i] = (Map)m.invoke(sample, (Object[])null);
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
		return submap((ObjectId)key).containsKey(key);
	}

	private final Map<ObjectId, V> submap(ObjectId key) {
		return level0[key.getFirstByte()];
	}

	public boolean containsValue(Object value) {
		for (int i=0; i<256; ++i)
			if (level0[i].containsValue(value))
				return true;
		return false;
	}

	private Set<Map.Entry<ObjectId, V>> entrySet =
		new AbstractSet<Map.Entry<ObjectId, V>>() {

			@Override
			final public Iterator<Map.Entry<ObjectId, V>> iterator() {
				return new Iterator<Entry<ObjectId,V>>() {
					private int levelIndex;
					private boolean hasNext;
					private Iterator<Map.Entry<ObjectId, V>> levelIterator;
					private Iterator<Map.Entry<ObjectId, V>> lastIterator;
					{
						step();
					}
					public boolean hasNext() {
						return hasNext;
					}
					public java.util.Map.Entry<ObjectId, V> next() {
						Entry<ObjectId, V> ret = levelIterator.next();
						step();
						return ret;
					}
					public void remove() {
						lastIterator.remove();
					}

					private void step() {
						hasNext = false;
						lastIterator = levelIterator;
						while ((levelIterator==null || !(hasNext=levelIterator.hasNext()))) {
							if (levelIndex < level0.length)
								levelIterator = level0[levelIndex++].entrySet().iterator();
							else
								break;
						}
					}
				};
			}

			@Override
			public int size() {
				int ret=0;
				for (int i=0; i<256; ++i)
					ret += level0[i].size();
				return ret;
			}
	};


	public Set<Map.Entry<ObjectId, V>> entrySet() {
		return entrySet;
	}

	public V get(Object key) {
		return submap((ObjectId)key).get(key);
	}

	public boolean isEmpty() {
		for (int i=0; i<256; ++i)
			if (!level0[i].isEmpty())
				return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public V put(ObjectId key, V value) {
		return submap(key).put(key, value);
	}

	public void putAll(Map<? extends ObjectId, ? extends V> arg0) {
		for (ObjectId k : arg0.keySet()) {
			V v=arg0.get(k);
			put(k,v);
		}
	}

	public V remove(Object key) {
		return submap((ObjectId) key).remove(key);
	}

	public int size() {
		return entrySet().size();
	}

}
