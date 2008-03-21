/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.revwalk;

import java.io.IOException;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.Tag;

/** An annotated tag. */
public class RevTag extends RevObject {
	private RevObject object;

	private byte[] buffer;

	private String name;

	/**
	 * Create a new tag reference.
	 * 
	 * @param id
	 *            object name for the tag.
	 */
	protected RevTag(final ObjectId id) {
		super(id);
	}

	void parse(final RevWalk walk) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		final ObjectLoader ldr = walk.db.openObject(walk.curs, this);
		if (ldr == null)
			throw new MissingObjectException(this, Constants.TYPE_TAG);
		final byte[] data = ldr.getCachedBytes();
		if (Constants.OBJ_TAG != ldr.getType())
			throw new IncorrectObjectTypeException(this, Constants.TYPE_TAG);
		parseCanonical(walk, data);
	}

	void parseCanonical(final RevWalk walk, final byte[] rawTag)
			throws CorruptObjectException {
		final int oType;
		int pos;

		switch (rawTag[53 + 0]) {
		case 'b':
			if (rawTag[53 + 1] != 'l' || rawTag[53 + 2] != 'o'
					|| rawTag[53 + 3] != 'b' || rawTag[53 + 4] != '\n')
				throw new CorruptObjectException(this, "invalid type");
			oType = Constants.OBJ_BLOB;
			pos = 53 + 5;
			break;
		case 'c':
			if (rawTag[53 + 1] != 'o' || rawTag[53 + 2] != 'm'
					|| rawTag[53 + 3] != 'm' || rawTag[53 + 4] != 'i'
					|| rawTag[53 + 5] != 't' || rawTag[53 + 6] != '\n')
				throw new CorruptObjectException(this, "invalid type");
			oType = Constants.OBJ_COMMIT;
			pos = 53 + 7;
			break;
		case 't':
			switch (rawTag[53 + 1]) {
			case 'a':
				if (rawTag[53 + 2] != 'g' || rawTag[53 + 3] != '\n')
					throw new CorruptObjectException(this, "invalid type");
				oType = Constants.OBJ_TAG;
				pos = 53 + 4;
				break;
			case 'r':
				if (rawTag[53 + 2] != 'e' || rawTag[53 + 3] != 'e'
						|| rawTag[53 + 4] != '\n')
					throw new CorruptObjectException(this, "invalid type");
				oType = Constants.OBJ_TREE;
				pos = 53 + 5;
				break;
			default:
				throw new CorruptObjectException(this, "invalid type");
			}
			break;
		default:
			throw new CorruptObjectException(this, "invalid type");
		}

		object = walk.lookupAny(ObjectId.fromString(rawTag, 7), oType);

		pos += 4;
		int nameEnd = pos;
		while (rawTag[nameEnd] != '\n')
			nameEnd++;
		final char[] nameBuf = new char[nameEnd - pos];
		for (int i = 0; i < nameBuf.length; i++)
			nameBuf[i] = (char) rawTag[pos++];

		name = new String(nameBuf);
		buffer = rawTag;
		flags |= PARSED;
	}

	/**
	 * Parse this tag buffer for display.
	 * 
	 * @param walk
	 *            revision walker owning this reference.
	 * @return parsed tag.
	 */
	public Tag asTag(final RevWalk walk) {
		return new Tag(walk.db, this, name, buffer);
	}

	/**
	 * Get a reference to the object this tag was placed on.
	 * 
	 * @return object this tag refers to.
	 */
	public RevObject getObject() {
		return object;
	}

	/**
	 * Get the name of this tag, from the tag header.
	 * 
	 * @return name of the tag, according to the tag header.
	 */
	public String getName() {
		return name;
	}

	public void dispose() {
		flags &= ~PARSED;
		buffer = null;
	}
}
