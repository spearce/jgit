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

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.EntryExistsException;
import org.spearce.jgit.errors.MissingObjectException;

public class Tree extends TreeEntry implements Treeish {
	public static final TreeEntry[] EMPTY_TREE = {};

	public static final int compareNames(final byte[] a, final byte[] b, final int lasta,final int lastb) {
		return compareNames(a, b, 0, b.length, lasta, lastb);
	}

	public static final int compareNames(final byte[] a, final byte[] nameUTF8,
			final int nameStart, final int nameEnd, final int lasta, int lastb) {
		int j,k;
		for (j = 0, k = nameStart; j < a.length && k < nameEnd; j++, k++) {
			final int aj = a[j] & 0xff;
			final int bk = nameUTF8[k] & 0xff;
			if (aj < bk)
				return -1;
			else if (aj > bk)
				return 1;
		}
		if (j < a.length) {
			int aj = a[j]&0xff;
			if (aj < lastb)
				return -1;
			else if (aj > lastb)
				return 1;
			else
				return 0;
		}
		if (k < nameEnd) {
			int bk = nameUTF8[k] & 0xff;
			if (lasta < bk)
				return -1;
			else if (lasta > bk)
				return 1;
			else
				return 0;
		}
		if (lasta < lastb)
			return -1;
		else if (lasta > lastb)
			return 1;

		final int namelength = nameEnd - nameStart;
		if (a.length == namelength)
			return 0;
		else if (a.length < namelength)
			return -1;
		else
			return 1;
	}

	private static final byte[] substring(final byte[] s, final int nameStart,
			final int nameEnd) {
		if (nameStart == 0 && nameStart == s.length)
			return s;
		final byte[] n = new byte[nameEnd - nameStart];
		System.arraycopy(s, nameStart, n, 0, n.length);
		return n;
	}

	private static final int binarySearch(final TreeEntry[] entries,
			final byte[] nameUTF8, final int nameUTF8last, final int nameStart, final int nameEnd) {
		if (entries.length == 0)
			return -1;
		int high = entries.length;
		int low = 0;
		do {
			final int mid = (low + high) / 2;
			final int cmp = compareNames(entries[mid].getNameUTF8(), nameUTF8,
					nameStart, nameEnd, TreeEntry.lastChar(entries[mid]), nameUTF8last);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp == 0)
				return mid;
			else
				high = mid;
		} while (low < high);
		return -(low + 1);
	}

	private final Repository db;

	private TreeEntry[] contents;

	public Tree(final Repository repo) {
		super(null, null, null);
		db = repo;
		contents = EMPTY_TREE;
	}

	public Tree(final Repository repo, final ObjectId myId, final byte[] raw)
			throws IOException {
		super(null, myId, null);
		db = repo;
		readTree(raw);
	}

	private Tree(final Tree parent, final byte[] nameUTF8) {
		super(parent, null, nameUTF8);
		db = parent.getRepository();
		contents = EMPTY_TREE;
	}

	public Tree(final Tree parent, final ObjectId id, final byte[] nameUTF8) {
		super(parent, id, nameUTF8);
		db = parent.getRepository();
	}

	public FileMode getMode() {
		return FileMode.TREE;
	}

	public boolean isRoot() {
		return getParent() == null;
	}

	public Repository getRepository() {
		return db;
	}

	public final ObjectId getTreeId() {
		return getId();
	}

	public final Tree getTree() {
		return this;
	}

	public boolean isLoaded() {
		return contents != null;
	}

	public void unload() {
		if (isModified())
			throw new IllegalStateException("Cannot unload a modified tree.");
		contents = null;
	}

	public FileTreeEntry addFile(final String name) throws IOException {
		return addFile(name.getBytes(Constants.CHARACTER_ENCODING), 0);
	}

	public FileTreeEntry addFile(final byte[] s, final int offset)
			throws IOException {
		int slash;
		int p;

		for (slash = offset; slash < s.length && s[slash] != '/'; slash++) {
			// search for path component terminator
		}

		ensureLoaded();
		byte xlast = slash<s.length ? (byte)'/' : 0;
		p = binarySearch(contents, s, xlast, offset, slash);
		if (p >= 0 && slash < s.length && contents[p] instanceof Tree)
			return ((Tree) contents[p]).addFile(s, slash + 1);

		final byte[] newName = substring(s, offset, slash);
		if (p >= 0)
			throw new EntryExistsException(new String(newName,
					Constants.CHARACTER_ENCODING));
		else if (slash < s.length) {
			final Tree t = new Tree(this, newName);
			insertEntry(p, t);
			return t.addFile(s, slash + 1);
		} else {
			final FileTreeEntry f = new FileTreeEntry(this, null, newName,
					false);
			insertEntry(p, f);
			return f;
		}
	}

	public Tree addTree(final String name) throws IOException {
		return addTree(name.getBytes(Constants.CHARACTER_ENCODING), 0);
	}

	public Tree addTree(final byte[] s, final int offset) throws IOException {
		int slash;
		int p;

		for (slash = offset; slash < s.length && s[slash] != '/'; slash++) {
			// search for path component terminator
		}

		ensureLoaded();
		p = binarySearch(contents, s, (byte)'/', offset, slash);
		if (p >= 0 && slash < s.length && contents[p] instanceof Tree)
			return ((Tree) contents[p]).addTree(s, slash + 1);

		final byte[] newName = substring(s, offset, slash);
		if (p >= 0)
			throw new EntryExistsException(new String(newName,
					Constants.CHARACTER_ENCODING));

		final Tree t = new Tree(this, newName);
		insertEntry(p, t);
		return slash == s.length ? t : t.addTree(s, slash + 1);
	}

	public void addEntry(final TreeEntry e) throws IOException {
		final int p;

		ensureLoaded();
		p = binarySearch(contents, e.getNameUTF8(), TreeEntry.lastChar(e), 0, e.getNameUTF8().length);
		if (p < 0) {
			e.attachParent(this);
			insertEntry(p, e);
		} else {
			throw new EntryExistsException(new String(e.getNameUTF8(),
					Constants.CHARACTER_ENCODING));
		}
	}

	private void insertEntry(int p, final TreeEntry e) {
		final TreeEntry[] c = contents;
		final TreeEntry[] n = new TreeEntry[c.length + 1];
		p = -(p + 1);
		for (int k = c.length - 1; k >= p; k--)
			n[k + 1] = c[k];
		n[p] = e;
		for (int k = p - 1; k >= 0; k--)
			n[k] = c[k];
		contents = n;
		setModified();
	}

	void removeEntry(final TreeEntry e) {
		final TreeEntry[] c = contents;
		final int p = binarySearch(c, e.getNameUTF8(), TreeEntry.lastChar(e), 0,
				e.getNameUTF8().length);
		if (p >= 0) {
			final TreeEntry[] n = new TreeEntry[c.length - 1];
			for (int k = c.length - 1; k > p; k--)
				n[k - 1] = c[k];
			for (int k = p - 1; k >= 0; k--)
				n[k] = c[k];
			contents = n;
			setModified();
		}
	}

	public int memberCount() throws IOException {
		ensureLoaded();
		return contents.length;
	}

	public TreeEntry[] members() throws IOException {
		ensureLoaded();
		final TreeEntry[] c = contents;
		if (c.length != 0) {
			final TreeEntry[] r = new TreeEntry[c.length];
			for (int k = c.length - 1; k >= 0; k--)
				r[k] = c[k];
			return r;
		} else
			return c;
	}

	public boolean exists(final String s, byte slast) throws IOException {
		return findMember(s, slast) != null;
	}

	public boolean existsTree(String path) throws IOException {
		return exists(path,(byte)'/');
	}

	public boolean existsBlob(String path) throws IOException {
		return exists(path,(byte)0);
	}

	public TreeEntry findMember(final String s, byte slast) throws IOException {
		return findMember(s.getBytes(Constants.CHARACTER_ENCODING), slast, 0);
	}

	public TreeEntry findMember(final byte[] s, final byte slast, final int offset)
			throws IOException {
		int slash;
		int p;

		for (slash = offset; slash < s.length && s[slash] != '/'; slash++) {
			// search for path component terminator
		}

		ensureLoaded();
		byte xlast = slash<s.length ? (byte)'/' : slast;
		p = binarySearch(contents, s, xlast, offset, slash);
		if (p >= 0) {
			final TreeEntry r = contents[p];
			if (slash < s.length)
				return r instanceof Tree ? ((Tree) r).findMember(s, slast, slash + 1)
						: null;
			return r;
		}
		return null;
	}

	public TreeEntry findBlobMember(String s) throws IOException {
		return findMember(s,(byte)0);
	}

	public TreeEntry findTreeMember(String s) throws IOException {
		return findMember(s,(byte)'/');
	}

	public void accept(final TreeVisitor tv, final int flags)
			throws IOException {
		final TreeEntry[] c;

		if ((MODIFIED_ONLY & flags) == MODIFIED_ONLY && !isModified())
			return;

		if ((LOADED_ONLY & flags) == LOADED_ONLY && !isLoaded()) {
			tv.startVisitTree(this);
			tv.endVisitTree(this);
			return;
		}

		ensureLoaded();
		tv.startVisitTree(this);

		if ((CONCURRENT_MODIFICATION & flags) == CONCURRENT_MODIFICATION)
			c = members();
		else
			c = contents;

		for (int k = 0; k < c.length; k++)
			c[k].accept(tv, flags);

		tv.endVisitTree(this);
	}

	private void ensureLoaded() throws IOException, MissingObjectException {
		if (!isLoaded()) {
			final ObjectLoader or = db.openTree(getId());
			if (or == null)
				throw new MissingObjectException(getId(), Constants.TYPE_TREE);
			readTree(or.getBytes());
		}
	}

	private void readTree(final byte[] raw) throws IOException {
		int rawPtr = 0;
		TreeEntry[] temp = new TreeEntry[64];
		int nextIndex = 0;

		while (rawPtr < raw.length) {
			int c = raw[rawPtr++] & 0xff;
			if (c < '0' || c > '7')
				throw new CorruptObjectException(getId(), "invalid entry mode");
			int mode = c - '0';
			for (;;) {
				c = raw[rawPtr++] & 0xff;
				if (' ' == c)
					break;
				else if (c < '0' || c > '7')
					throw new CorruptObjectException(getId(), "invalid mode");
				mode <<= 3;
				mode += c - '0';
			}

			int nameLen = 0;
			while ((raw[rawPtr + nameLen] & 0xff) != 0)
				nameLen++;
			final byte[] name = new byte[nameLen];
			System.arraycopy(raw, rawPtr, name, 0, nameLen);
			rawPtr += nameLen + 1;

			final byte[] entId = new byte[Constants.OBJECT_ID_LENGTH];
			System.arraycopy(raw, rawPtr, entId, 0, Constants.OBJECT_ID_LENGTH);
			final ObjectId id = new ObjectId(entId);
			rawPtr += Constants.OBJECT_ID_LENGTH;

			final TreeEntry ent;
			if (FileMode.REGULAR_FILE.equals(mode))
				ent = new FileTreeEntry(this, id, name, false);
			else if (FileMode.EXECUTABLE_FILE.equals(mode))
				ent = new FileTreeEntry(this, id, name, true);
			else if (FileMode.TREE.equals(mode)) {
				ent = new Tree(this, id, name);
			} else if (FileMode.SYMLINK.equals(mode))
				ent = new SymlinkTreeEntry(this, id, name);
			else
				throw new CorruptObjectException(getId(), "Invalid mode: "
						+ Integer.toOctalString(mode));

			if (nextIndex == temp.length) {
				final TreeEntry[] n = new TreeEntry[temp.length << 1];
				for (int k = nextIndex - 1; k >= 0; k--)
					n[k] = temp[k];
				temp = n;
			}

			temp[nextIndex++] = ent;
		}

		if (nextIndex == temp.length)
			contents = temp;
		else {
			final TreeEntry[] n = new TreeEntry[nextIndex];
			for (int k = nextIndex - 1; k >= 0; k--)
				n[k] = temp[k];
			contents = n;
		}

	}

	public String toString() {
		final StringBuffer r = new StringBuffer();
		r.append(ObjectId.toString(getId()));
		r.append(" T ");
		r.append(getFullName());
		return r.toString();
	}

}