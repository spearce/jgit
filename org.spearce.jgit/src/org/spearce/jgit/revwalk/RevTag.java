/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
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
		final int nameEnd = RawParseUtils.nextLF(rawTag, p) - 1;
		name = RawParseUtils.decode(Constants.CHARSET, rawTag, p, nameEnd);
		buffer = rawTag;
		flags |= PARSED;
	}

	@Override
	public int getType() {
		return Constants.OBJ_TAG;
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
