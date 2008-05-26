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

package org.spearce.jgit.lib;

/**
 * Pairing of a name and the {@link ObjectId} it currently has.
 * <p>
 * A ref in Git is (more or less) a variable that holds a single object
 * identifier. The object identifier can be any valid Git object (blob, tree,
 * commit, annotated tag, ...).
 */
public class Ref {
	private final String name;

	private ObjectId objectId;

	private ObjectId peeledObjectId;

	/**
	 * Create a new ref pairing.
	 * 
	 * @param refName
	 *            name of this ref.
	 * @param id
	 *            current value of the ref. May be null to indicate a ref that
	 *            does not exist yet.
	 */
	public Ref(final String refName, final ObjectId id) {
		name = refName;
		objectId = id;
	}

	/**
	 * Create a new ref pairing.
	 * 
	 * @param refName
	 *            name of this ref.
	 * @param id
	 *            current value of the ref. May be null to indicate a ref that
	 *            does not exist yet.
	 * @param peel
	 *            peeled value of the ref's tag. May be null if this is not a
	 *            tag or the peeled value is not known.
	 */
	public Ref(final String refName, final ObjectId id, final ObjectId peel) {
		name = refName;
		objectId = id;
		peeledObjectId = peel;
	}

	/**
	 * What this ref is called within the repository.
	 * 
	 * @return name of this ref.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Cached value of this ref.
	 * 
	 * @return the value of this ref at the last time we read it.
	 */
	public ObjectId getObjectId() {
		return objectId;
	}

	/**
	 * Cached value of <code>ref^{}</code> (the ref peeled to commit).
	 * 
	 * @return if this ref is an annotated tag the id of the commit (or tree or
	 *         blob) that the annotated tag refers to; null if this ref does not
	 *         refer to an annotated tag.
	 */
	public ObjectId getPeeledObjectId() {
		return peeledObjectId;
	}

	public String toString() {
		return "Ref[" + name + "=" + getObjectId() + "]";
	}
}
