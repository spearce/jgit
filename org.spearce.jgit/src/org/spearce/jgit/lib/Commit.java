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

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.MissingObjectException;

/**
 * Instances of this class represent a Commit object. It represents a snapshot
 * in a Git repository, who created it and when.
 */
public class Commit implements Treeish {
	private static final ObjectId[] EMPTY_OBJECTID_LIST = new ObjectId[0];

	private final Repository objdb;

	private ObjectId commitId;

	private ObjectId treeId;

	private ObjectId[] parentIds;
	
	private PersonIdent author;

	private PersonIdent committer;

	private String message;

	private Tree treeObj;

	private byte[] raw;

	private Charset encoding;

	/**
	 * Create an empty commit object. More information must be fed to this
	 * object to make it useful.
	 *
	 * @param db
	 *            The repository with which to associate it.
	 */
	public Commit(final Repository db) {
		objdb = db;
		parentIds = EMPTY_OBJECTID_LIST;
	}
	
	/**
	 * Create a commit associated with these parents and associate it with a
	 * repository.
	 *
	 * @param db
	 *            The repository to which this commit object belongs
	 * @param parentIds
	 *            Id's of the parent(s)
	 */
	public Commit(final Repository db, final ObjectId[] parentIds) {
		objdb = db;
		this.parentIds = parentIds;
	}

	/**
	 * Create a commit object with the specified id and data from and existing
	 * commit object in a repository.
	 *
	 * @param db
	 *            The repository to which this commit object belongs
	 * @param id
	 *            Commit id
	 * @param raw
	 *            Raw commit object data
	 */
	public Commit(final Repository db, final ObjectId id, final byte[] raw) {
		objdb = db;
		commitId = id;
		treeId = ObjectId.fromString(raw, 5);
		parentIds = new ObjectId[1];
		int np=0;
		int rawPtr = 46;
		for (;;) {
			if (raw[rawPtr] != 'p')
				break;
			if (np == 0) {
				parentIds[np++] = ObjectId.fromString(raw, rawPtr + 7);
			} else if (np == 1) {
				parentIds = new ObjectId[] { parentIds[0], ObjectId.fromString(raw, rawPtr + 7) };
				np++;
			} else {
				if (parentIds.length <= np) {
					ObjectId[] old = parentIds;
					parentIds = new ObjectId[parentIds.length+32];
					for (int i=0; i<np; ++i)
						parentIds[i] = old[i];
				}
				parentIds[np++] = ObjectId.fromString(raw, rawPtr + 7);
			}
			rawPtr += 48;
		}
		if (np != parentIds.length) {
			ObjectId[] old = parentIds;
			parentIds = new ObjectId[np];
			for (int i=0; i<np; ++i)
				parentIds[i] = old[i];
		} else
			if (np == 0)
				parentIds = EMPTY_OBJECTID_LIST;
		this.raw = raw;
	}

	/**
	 * @return The commit object id
	 */
	public ObjectId getCommitId() {
		return commitId;
	}

	/**
	 * Set the id of this object.
	 *
	 * @param id
	 *            the id that we calculated for this object.
	 */
	public void setCommitId(final ObjectId id) {
		commitId = id;
	}

	public ObjectId getTreeId() {
		return treeId;
	}

	/**
	 * Set the tree id for this commit object
	 *
	 * @param id
	 */
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

	/**
	 * Set the tree object for this commit
	 * @see #setTreeId
	 * @param t the Tree object
	 */
	public void setTree(final Tree t) {
		treeId = t.getTreeId();
		treeObj = t;
	}

	/**
	 * @return the author and authoring time for this commit
	 */
	public PersonIdent getAuthor() {
		decode();
		return author;
	}

	/**
	 * Set the author and authoring time for this commit
	 * @param a
	 */
	public void setAuthor(final PersonIdent a) {
		author = a;
	}

	/**
	 * @return the committer and commit time for this object
	 */
	public PersonIdent getCommitter() {
		decode();
		return committer;
	}

	/**
	 * Set the committer and commit time for this object
	 * @param c the committer information
	 */
	public void setCommitter(final PersonIdent c) {
		committer = c;
	}

	/**
	 * @return the object ids of this commit
	 */
	public ObjectId[] getParentIds() {
		return parentIds;
	}

	/**
	 * @return the commit message
	 */
	public String getMessage() {
		decode();
		return message;
	}

	/**
	 * Set the parents of this commit
	 * @param parentIds
	 */
	public void setParentIds(ObjectId[] parentIds) {
		this.parentIds = new ObjectId[parentIds.length];
		for (int i=0; i<parentIds.length; ++i)
			this.parentIds[i] = parentIds[i];
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
				int msgstart = readBuf.length != 0 ? ( readBuf[0] == '\n' ? 1 : 0 ) : 0;

				if (encoding != null) {
					// TODO: this isn't reliable so we need to guess the encoding from the actual content
					author = new PersonIdent(new String(rawAuthor.getBytes(),encoding.name()));
					committer = new PersonIdent(new String(rawCommitter.getBytes(),encoding.name()));
					message = new String(readBuf,msgstart, readBuf.length-msgstart, encoding.name());
				} else {
					// TODO: use config setting / platform / ascii / iso-latin
					author = new PersonIdent(new String(rawAuthor.getBytes()));
					committer = new PersonIdent(new String(rawCommitter.getBytes()));
					message = new String(readBuf, msgstart, readBuf.length-msgstart);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				raw = null;
			}
		}
	}

	/**
	 * Set the commit message
	 *
	 * @param m the commit message
	 */
	public void setMessage(final String m) {
		message = m;
	}

	/**
	 * Persist this commit object
	 *
	 * @throws IOException
	 */
	public void commit() throws IOException {
		if (getCommitId() != null)
			throw new IllegalStateException("exists " + getCommitId());
		setCommitId(new ObjectWriter(objdb).writeCommit(this));
	}

	public String toString() {
		return "Commit[" + getCommitId() + " " + getAuthor() + "]";
	}

	/**
	 * State the encoding for the commit information
	 *
	 * @param e
	 *            the encoding. See {@link Charset}
	 */
	public void setEncoding(String e) {
		encoding = Charset.forName(e);
	}

	/**
	 * State the encoding for the commit information
	 *
	 * @param e
	 *            the encoding. See {@link Charset}
	 */
	public void setEncoding(Charset e) {
		encoding = e;
	}

	/**
	 * @return the encoding used. See {@link Charset}
	 */
	public String getEncoding() {
		if (encoding != null)
			return encoding.name();
		else
			return null;
	}
}
