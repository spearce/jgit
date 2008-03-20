/*
 *  Copyright (C) 20067  Robin Rosenberg <robin.rosenberg@dewire.com>
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.ObjectWritingException;

/**
 * Represents a named reference to another Git object of any type.
 */
public class Tag {
	private final Repository objdb;

	private ObjectId tagId;

	private PersonIdent tagger;

	private String message;

	private byte[] raw;

	private String type;

	private String tag;

	private ObjectId objId;

	/**
	 * Construct a new, yet unnamed Tag.
	 *
	 * @param db
	 */
	public Tag(final Repository db) {
		objdb = db;
	}

	/**
	 * Construct a Tag representing an existing with a know name referencing an known object.
	 * This could be either a simple or annotated tag.
	 *
	 * @param db {@link Repository}
	 * @param id target id.
	 * @param refName tag name
	 * @param raw data of an annotated tag.
	 */
	public Tag(final Repository db, final ObjectId id, String refName, final byte[] raw) {
		objdb = db;
		if (raw != null) {
			tagId = id;
			objId = ObjectId.fromString(raw, 7);
		} else
			objId = id;
		if (refName.startsWith("refs/tags/"))
			refName = refName.substring(10);
		tag = refName;
		this.raw = raw;
	}

	/**
	 * @return tagger of a annotated tag or null
	 */
	public PersonIdent getAuthor() {
		decode();
		return tagger;
	}

	/**
	 * Set author of an annotated tag.
	 * @param a author identifier as a {@link PersonIdent}
	 */
	public void setAuthor(final PersonIdent a) {
		tagger = a;
	}

	/**
	 * @return comment of an annotated tag, or null
	 */
	public String getMessage() {
		decode();
		return message;
	}

	private void decode() {
		// FIXME: handle I/O errors
		if (raw != null) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						new ByteArrayInputStream(raw)));
				String n = br.readLine();
				if (n == null || !n.startsWith("object ")) {
					throw new CorruptObjectException(tagId, "no object");
				}
				objId = ObjectId.fromString(n.substring(7));
				n = br.readLine();
				if (n == null || !n.startsWith("type ")) {
					throw new CorruptObjectException(tagId, "no type");
				}
				type = n.substring("type ".length());
				n = br.readLine();

				if (n == null || !n.startsWith("tag ")) {
					throw new CorruptObjectException(tagId, "no tag name");
				}
				tag = n.substring("tag ".length());
				n = br.readLine();

				// We should see a "tagger" header here, but some repos have tags
				// without it.
				if (n == null)
					throw new CorruptObjectException(tagId, "no tagger header");

				if (n.length()>0)
					if (n.startsWith("tagger "))
						tagger = new PersonIdent(n.substring("tagger ".length()));
					else
						throw new CorruptObjectException(tagId, "no tagger/bad header");

				// Message should start with an empty line, but
				StringBuffer tempMessage = new StringBuffer();
				char[] readBuf = new char[2048];
				int readLen;
				while ((readLen = br.read(readBuf)) > 0) {
					tempMessage.append(readBuf, 0, readLen);
				}
				message = tempMessage.toString();
				if (message.startsWith("\n"))
					message = message.substring(1);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				raw = null;
			}
		}
	}

	/**
	 * Set the message of an annotated tag
	 * @param m
	 */
	public void setMessage(final String m) {
		message = m;
	}

	/**
	 * Store a tag.
	 * If author, message or type is set make the tag an annotated tag.
	 *
	 * @throws IOException
	 */
	public void tag() throws IOException {
		if (getTagId() != null)
			throw new IllegalStateException("exists " + getTagId());
		final ObjectId id;
		if (tagger!=null || message!=null || type!=null) {
			ObjectId tagid = new ObjectWriter(objdb).writeTag(this);
			setTagId(tagid);
			id = tagid;
		} else {
			id = objId;
		}
		final RefLock lck = objdb.lockRef("refs/tags/" + getTag());
		if (lck == null)
			throw new ObjectWritingException("Unable to lock tag " + getTag());
		lck.write(id);
		if (!lck.commit())
			throw new ObjectWritingException("Unable to write tag " + getTag());
	}

	public String toString() {
		return "tag[" + getTag() + getType() + getObjId() + " " + getAuthor() + "]";
	}

	/**
	 * @return SHA-1 of this tag (if annotated and stored).
	 */
	public ObjectId getTagId() {
		return tagId;
	}

	/**
	 * Set SHA-1 of this tag. Used by writer.
	 *
	 * @param tagId
	 */
	public void setTagId(ObjectId tagId) {
		this.tagId = tagId;
	}

	/**
	 * @return creator of this tag.
	 */
	public PersonIdent getTagger() {
		decode();
		return tagger;
	}

	/**
	 * Set the creator of this tag
	 *
	 * @param tagger
	 */
	public void setTagger(PersonIdent tagger) {
		this.tagger = tagger;
	}

	/**
	 * @return tag target type
	 */
	public String getType() {
		decode();
		return type;
	}

	/**
	 * Set tag target type
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return name of the tag.
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Set the name of this tag.
	 *
	 * @param tag
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the SHA'1 of the object this tag refers to.
	 */
	public ObjectId getObjId() {
		return objId;
	}

	/**
	 * Set the id of the object this tag refers to.
	 *
	 * @param objId
	 */
	public void setObjId(ObjectId objId) {
		this.objId = objId;
	}
}
