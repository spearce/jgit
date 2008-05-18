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
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.Tag;
import org.spearce.jgit.util.MutableInteger;
import org.spearce.jgit.util.RawParseUtils;

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
	protected RevTag(final AnyObjectId id) {
		super(id);
	}

	@Override
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
		final MutableInteger pos = new MutableInteger();
		final int oType;

		pos.value = 53; // "object $sha1\ntype "
		oType = Constants.decodeTypeString(this, rawTag, (byte) '\n', pos);
		walk.idBuffer.fromString(rawTag, 7);
		object = walk.lookupAny(walk.idBuffer, oType);

		int p = pos.value += 4; // "tag "
		final int nameEnd = RawParseUtils.next(rawTag, p, '\n') - 1;
		name = RawParseUtils.decode(Constants.CHARSET, rawTag, p, nameEnd);
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
