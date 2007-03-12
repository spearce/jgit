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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.MissingObjectException;

public class Commit implements Treeish {
	private final Repository objdb;

	private ObjectId commitId;

	private ObjectId treeId;

	private List parentIds;

	private PersonIdent author;

	private PersonIdent committer;

	private String message;

	private Tree treeObj;

	private byte[] raw;

	private Charset encoding;

	public Commit(final Repository db) {
		objdb = db;
		parentIds = new ArrayList(2);
	}

	public Commit(final Repository db, final ObjectId id, final byte[] raw) {
		objdb = db;
		commitId = id;
		treeId = ObjectId.fromString(raw, 5);
		parentIds = new ArrayList(2);
		int rawPtr = 46;
		for (;;) {
			if (raw[rawPtr] != 'p')
				break;
			parentIds.add(ObjectId.fromString(raw, rawPtr + 7));
			rawPtr += 48;
		}

		this.raw = raw;
	}

	public ObjectId getCommitId() {
		return commitId;
	}

	public void setCommitId(final ObjectId id) {
		commitId = id;
	}

	public ObjectId getTreeId() {
		return treeId;
	}

	public void setTreeId(final ObjectId id) {
		if (treeId==null || !treeId.equals(id)) {
			treeObj = null;
		}
		treeId = id;
	}

	public Tree getTree() throws IOException {
		if (treeObj == null) {
			treeObj = objdb.mapTree(getTreeId());
			if (treeObj == null) {
				throw new MissingObjectException(getTreeId(),
						Constants.TYPE_TREE);
			}
		}
		return treeObj;
	}

	public void setTree(final Tree t) {
		treeId = t.getTreeId();
		treeObj = t;
	}

	public PersonIdent getAuthor() {
		decode();
		return author;
	}

	public void setAuthor(final PersonIdent a) {
		author = a;
	}

	public PersonIdent getCommitter() {
		decode();
		return committer;
	}

	public void setCommitter(final PersonIdent c) {
		committer = c;
	}

	public List getParentIds() {
		return parentIds;
	}

	public String getMessage() {
		decode();
		return message;
	}

	private void decode() {
		// FIXME: handle I/O errors
		if (raw != null) {
			try {
				DataInputStream br = new DataInputStream(new ByteArrayInputStream(raw));
				String n = br.readLine();
				if (n == null || !n.startsWith("tree ")) {
					throw new CorruptObjectException(commitId, "no tree");
				}
				while ((n = br.readLine()) != null && n.startsWith("parent ")) {
					// empty body
				}
				if (n == null || !n.startsWith("author ")) {
					throw new CorruptObjectException(commitId, "no author");
				}
				String rawAuthor = n.substring("author ".length());
				n = br.readLine();
				if (n == null || !n.startsWith("committer ")) {
					throw new CorruptObjectException(commitId, "no committer");
				}
				String rawCommitter = n.substring("committer ".length());
				n = br.readLine();
				if (n != null && n.startsWith(	"encoding"))
					encoding = Charset.forName(n.substring("encoding ".length()));
				else
					if (n == null || !n.equals("")) {
						throw new CorruptObjectException(commitId,
								"malformed header:"+n);
				}
				byte[] readBuf = new byte[br.available()]; // in-memory stream so this is all bytes left
				br.read(readBuf);
				if (encoding != null) {
					// TODO: this isn't reliable so we need to guess the encoding from the actual content
					author = new PersonIdent(new String(rawAuthor.getBytes(),encoding.name()));
					committer = new PersonIdent(new String(rawCommitter.getBytes(),encoding.name()));
					message = new String(readBuf,encoding.name());
				} else {
					// TODO: use config setting / platform / ascii / iso-latin
					author = new PersonIdent(new String(rawAuthor.getBytes()));
					committer = new PersonIdent(new String(rawCommitter.getBytes()));
					message = new String(readBuf);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				raw = null;
			}
		}
	}

	public void setMessage(final String m) {
		message = m;
	}

	public void commit() throws IOException {
		if (getCommitId() != null)
			throw new IllegalStateException("exists " + getCommitId());
		setCommitId(new ObjectWriter(objdb).writeCommit(this));
	}

	public String toString() {
		return "Commit[" + getCommitId() + " " + getAuthor() + "]";
	}

	public void setEncoding(String e) {
		encoding = Charset.forName(e);
	}

	public void setEncoding(Charset e) {
		encoding = e;
	}

	public String getEncoding() {
		if (encoding != null)
			return encoding.name();
		else
			return null;
	}
}
