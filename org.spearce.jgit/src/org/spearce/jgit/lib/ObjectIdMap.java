/*
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

	public V put(ObjectId key, V value) {
		return submap(key).put(key, value);
	}

	public void putAll(Map<? extends ObjectId, ? extends V> arg0) {
		for (Map.Entry<? extends ObjectId, ? extends V> entry : arg0.entrySet()) {
			final ObjectId k = entry.getKey();
			final V v = entry.getValue();
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
