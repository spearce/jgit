/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This class represents an entry in a tree, like a blob or another tree.
 */
public abstract class TreeEntry implements Comparable {
	/**
	 * a flag for {@link TreeEntry#accept(TreeVisitor, int)} to visit only modified entries
	 */
	public static final int MODIFIED_ONLY = 1 << 0;

	/**
	 * a flag for {@link TreeEntry#accept(TreeVisitor, int)} to visit only loaded entries
	 */
	public static final int LOADED_ONLY = 1 << 1;

	/**
	 * a flag for {@link TreeEntry#accept(TreeVisitor, int)} obsolete?
	 */
	public static final int CONCURRENT_MODIFICATION = 1 << 2;

	private byte[] nameUTF8;

	private Tree parent;

	private ObjectId id;

	/**
	 * Construct a named tree entry.
	 *
	 * @param myParent
	 * @param myId
	 * @param myNameUTF8
	 */
	protected TreeEntry(final Tree myParent, final ObjectId myId,
			final byte[] myNameUTF8) {
		nameUTF8 = myNameUTF8;
		parent = myParent;
		id = myId;
	}

	/**
	 * @return parent of this tree.
	 */
	public Tree getParent() {
		return parent;
	}

	/**
	 * Delete this entry.
	 */
	public void delete() {
		getParent().removeEntry(this);
		detachParent();
	}

	/**
	 * Detach this entry from it's parent.
	 */
	public void detachParent() {
		parent = null;
	}

	void attachParent(final Tree p) {
		parent = p;
	}

	/**
	 * @return the repository owning this entry.
	 */
	public Repository getRepository() {
		return getParent().getRepository();
	}

	/**
	 * @return the raw byte name of this entry.
	 */
	public byte[] getNameUTF8() {
		return nameUTF8;
	}

	/**
	 * @return the name of this entry.
	 */
	public String getName() {
		try {
			return nameUTF8 != null ? new String(nameUTF8,
					Constants.CHARACTER_ENCODING) : null;
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("JVM doesn't support "
					+ Constants.CHARACTER_ENCODING, uee);
		}
	}

	/**
	 * Rename this entry.
	 *
	 * @param n The new name
	 * @throws IOException
	 */
	public void rename(final String n) throws IOException {
		rename(n.getBytes(Constants.CHARACTER_ENCODING));
	}

	/**
	 * Rename this entry.
	 *
	 * @param n The new name
	 * @throws IOException
	 */
	public void rename(final byte[] n) throws IOException {
		final Tree t = getParent();
		if (t != null) {
			delete();
		}
		nameUTF8 = n;
		if (t != null) {
			t.addEntry(this);
		}
	}

	/**
	 * @return true if this entry is new or modified since being loaded.
	 */
	public boolean isModified() {
		return getId() == null;
	}

	/**
	 * Mark this entry as modified.
	 */
	public void setModified() {
		setId(null);
	}

	/**
	 * @return SHA-1 of this tree entry (null for new unhashed entries)
	 */
	public ObjectId getId() {
		return id;
	}

	/**
	 * Set (update) the SHA-1 of this entry. Invalidates the id's of all
	 * entries above this entry as they will have to be recomputed.
	 *
	 * @param n SHA-1 for this entry.
	 */
	public void setId(final ObjectId n) {
		// If we have a parent and our id is being cleared or changed then force
		// the parent's id to become unset as it depends on our id.
		//
		final Tree p = getParent();
		if (p != null && id != n) {
			if ((id == null && n != null) || (id != null && n == null)
					|| !id.equals(n)) {
				p.setId(null);
			}
		}

		id = n;
	}

	/**
	 * @return repository relative name of this entry
	 */
	public String getFullName() {
		final StringBuffer r = new StringBuffer();
		appendFullName(r);
		return r.toString();
	}

	public int compareTo(final Object o) {
		if (this == o)
			return 0;
		if (o instanceof TreeEntry)
			return Tree.compareNames(nameUTF8, ((TreeEntry) o).nameUTF8, lastChar(this), lastChar((TreeEntry)o));
		return -1;
	}

	/**
	 * Helper for accessing tree/blob methods.
	 *
	 * @param treeEntry
	 * @return '/' for Tree entries and NUL for non-treeish objects.
	 */
	final public static int lastChar(TreeEntry treeEntry) {
		if (treeEntry instanceof FileTreeEntry)
			return '\0';
		else
			return '/';
	}

	/**
	 * See @{link {@link #accept(TreeVisitor, int)}.
	 *
	 * @param tv
	 * @throws IOException
	 */
	public void accept(final TreeVisitor tv) throws IOException {
		accept(tv, 0);
	}

	/**
	 * Visit the members of this TreeEntry.
	 *
	 * @param tv
	 *            A visitor object doing the work
	 * @param flags
	 *            Specification for what members to visit. See
	 *            {@link #MODIFIED_ONLY}, {@link #LOADED_ONLY},
	 *            {@link #CONCURRENT_MODIFICATION}.
	 * @throws IOException
	 */
	public abstract void accept(TreeVisitor tv, int flags) throws IOException;

	/**
	 * @return mode (type of object)
	 */
	public abstract FileMode getMode();

	private void appendFullName(final StringBuffer r) {
		final TreeEntry p = getParent();
		final String n = getName();
		if (p != null) {
			p.appendFullName(r);
			if (r.length() > 0) {
				r.append('/');
			}
		}
		if (n != null) {
			r.append(n);
		}
	}
}
