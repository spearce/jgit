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

public abstract class TreeEntry implements Comparable {
	public static final int MODIFIED_ONLY = 1 << 0;

	public static final int LOADED_ONLY = 1 << 1;

	public static final int CONCURRENT_MODIFICATION = 1 << 2;

	private byte[] nameUTF8;

	private Tree parent;

	private ObjectId id;

	protected TreeEntry(final Tree myParent, final ObjectId myId,
			final byte[] myNameUTF8) {
		nameUTF8 = myNameUTF8;
		parent = myParent;
		id = myId;
	}

	public Tree getParent() {
		return parent;
	}

	public void delete() {
		getParent().removeEntry(this);
		detachParent();
	}

	public void detachParent() {
		parent = null;
	}

	void attachParent(final Tree p) {
		parent = p;
	}

	public Repository getRepository() {
		return getParent().getRepository();
	}

	public byte[] getNameUTF8() {
		return nameUTF8;
	}

	public String getName() {
		try {
			return nameUTF8 != null ? new String(nameUTF8,
					Constants.CHARACTER_ENCODING) : null;
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("JVM doesn't support "
					+ Constants.CHARACTER_ENCODING, uee);
		}
	}

	public void rename(final String n) throws IOException {
		rename(n.getBytes(Constants.CHARACTER_ENCODING));
	}

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

	public boolean isModified() {
		return getId() == null;
	}

	public void setModified() {
		setId(null);
	}

	public ObjectId getId() {
		return id;
	}

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

	public String getFullName() {
		final StringBuffer r = new StringBuffer();
		appendFullName(r);
		return r.toString();
	}

	public int compareTo(final Object o) {
		if (this == o)
			return 0;
		if (o instanceof TreeEntry)
			return Tree.compareNames(nameUTF8, ((TreeEntry) o).nameUTF8);
		return -1;
	}

	public void accept(final TreeVisitor tv) throws IOException {
		accept(tv, 0);
	}

	public abstract void accept(TreeVisitor tv, int flags) throws IOException;

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
