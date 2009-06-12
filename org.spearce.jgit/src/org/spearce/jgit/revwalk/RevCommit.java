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
import java.nio.charset.Charset;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.MutableObjectId;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.util.RawParseUtils;

/** A commit reference to a commit in the DAG. */
public class RevCommit extends RevObject {
	static final RevCommit[] NO_PARENTS = {};

	private RevTree tree;

	RevCommit[] parents;

	int commitTime; // An int here for performance, overflows in 2038

	int inDegree;

	private byte[] buffer;

	/**
	 * Create a new commit reference.
	 * 
	 * @param id
	 *            object name for the commit.
	 */
	protected RevCommit(final AnyObjectId id) {
		super(id);
	}

	@Override
	void parseHeaders(final RevWalk walk) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		parseCanonical(walk, loadCanonical(walk));
	}

	@Override
	void parseBody(final RevWalk walk) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if (buffer == null) {
			buffer = loadCanonical(walk);
			if ((flags & PARSED) == 0)
				parseCanonical(walk, buffer);
		}
	}

	void parseCanonical(final RevWalk walk, final byte[] raw) {
		final MutableObjectId idBuffer = walk.idBuffer;
		idBuffer.fromString(raw, 5);
		tree = walk.lookupTree(idBuffer);

		int ptr = 46;
		if (parents == null) {
			RevCommit[] pList = new RevCommit[1];
			int nParents = 0;
			for (;;) {
				if (raw[ptr] != 'p')
					break;
				idBuffer.fromString(raw, ptr + 7);
				final RevCommit p = walk.lookupCommit(idBuffer);
				if (nParents == 0)
					pList[nParents++] = p;
				else if (nParents == 1) {
					pList = new RevCommit[] { pList[0], p };
					nParents = 2;
				} else {
					if (pList.length <= nParents) {
						RevCommit[] old = pList;
						pList = new RevCommit[pList.length + 32];
						System.arraycopy(old, 0, pList, 0, nParents);
					}
					pList[nParents++] = p;
				}
				ptr += 48;
			}
			if (nParents != pList.length) {
				RevCommit[] old = pList;
				pList = new RevCommit[nParents];
				System.arraycopy(old, 0, pList, 0, nParents);
			}
			parents = pList;
		}

		// extract time from "committer "
		ptr = RawParseUtils.committer(raw, ptr);
		if (ptr > 0) {
			ptr = RawParseUtils.nextLF(raw, ptr, '>');

			// In 2038 commitTime will overflow unless it is changed to long.
			commitTime = RawParseUtils.parseBase10(raw, ptr, null);
		}

		if (walk.isRetainBody())
			buffer = raw;
		flags |= PARSED;
	}
	
	@Override
	public final int getType() {
		return Constants.OBJ_COMMIT;
	}

	static void carryFlags(RevCommit c, final int carry) {
		for (;;) {
			final RevCommit[] pList = c.parents;
			if (pList == null)
				return;
			final int n = pList.length;
			if (n == 0)
				return;

			for (int i = 1; i < n; i++) {
				final RevCommit p = pList[i];
				if ((p.flags & carry) == carry)
					continue;
				p.flags |= carry;
				carryFlags(p, carry);
			}

			c = pList[0];
			if ((c.flags & carry) == carry)
				return;
			c.flags |= carry;
		}
	}

	/**
	 * Carry a RevFlag set on this commit to its parents.
	 * <p>
	 * If this commit is parsed, has parents, and has the supplied flag set on
	 * it we automatically add it to the parents, grand-parents, and so on until
	 * an unparsed commit or a commit with no parents is discovered. This
	 * permits applications to force a flag through the history chain when
	 * necessary.
	 * 
	 * @param flag
	 *            the single flag value to carry back onto parents.
	 */
	public void carry(final RevFlag flag) {
		final int carry = flags & flag.mask;
		if (carry != 0)
			carryFlags(this, carry);
	}

	/**
	 * Time from the "committer " line of the buffer.
	 * 
	 * @return time, expressed as seconds since the epoch.
	 */
	public final int getCommitTime() {
		return commitTime;
	}

	/**
	 * Parse this commit buffer for display.
	 * 
	 * @param walk
	 *            revision walker owning this reference.
	 * @return parsed commit.
	 */
	public final Commit asCommit(final RevWalk walk) {
		return new Commit(walk.db, this, buffer);
	}

	/**
	 * Get a reference to this commit's tree.
	 * 
	 * @return tree of this commit.
	 */
	public final RevTree getTree() {
		return tree;
	}

	/**
	 * Get the number of parent commits listed in this commit.
	 * 
	 * @return number of parents; always a positive value but can be 0.
	 */
	public final int getParentCount() {
		return parents.length;
	}

	/**
	 * Get the nth parent from this commit's parent list.
	 * 
	 * @param nth
	 *            parent index to obtain. Must be in the range 0 through
	 *            {@link #getParentCount()}-1.
	 * @return the specified parent.
	 * @throws ArrayIndexOutOfBoundsException
	 *             an invalid parent index was specified.
	 */
	public final RevCommit getParent(final int nth) {
		return parents[nth];
	}

	/**
	 * Obtain an array of all parents (<b>NOTE - THIS IS NOT A COPY</b>).
	 * <p>
	 * This method is exposed only to provide very fast, efficient access to
	 * this commit's parent list. Applications relying on this list should be
	 * very careful to ensure they do not modify its contents during their use
	 * of it.
	 * 
	 * @return the array of parents.
	 */
	public final RevCommit[] getParents() {
		return parents;
	}

	/**
	 * Obtain the raw unparsed commit body (<b>NOTE - THIS IS NOT A COPY</b>).
	 * <p>
	 * This method is exposed only to provide very fast, efficient access to
	 * this commit's message buffer within a RevFilter. Applications relying on
	 * this buffer should be very careful to ensure they do not modify its
	 * contents during their use of it.
	 * 
	 * @return the raw unparsed commit body. This is <b>NOT A COPY</b>.
	 *         Altering the contents of this buffer may alter the walker's
	 *         knowledge of this commit, and the results it produces.
	 */
	public final byte[] getRawBuffer() {
		return buffer;
	}

	/**
	 * Parse the author identity from the raw buffer.
	 * <p>
	 * This method parses and returns the content of the author line, after
	 * taking the commit's character set into account and decoding the author
	 * name and email address. This method is fairly expensive and produces a
	 * new PersonIdent instance on each invocation. Callers should invoke this
	 * method only if they are certain they will be outputting the result, and
	 * should cache the return value for as long as necessary to use all
	 * information from it.
	 * <p>
	 * RevFilter implementations should try to use {@link RawParseUtils} to scan
	 * the {@link #getRawBuffer()} instead, as this will allow faster evaluation
	 * of commits.
	 * 
	 * @return identity of the author (name, email) and the time the commit was
	 *         made by the author; null if no author line was found.
	 */
	public final PersonIdent getAuthorIdent() {
		final byte[] raw = buffer;
		final int nameB = RawParseUtils.author(raw, 0);
		if (nameB < 0)
			return null;
		return RawParseUtils.parsePersonIdent(raw, nameB);
	}

	/**
	 * Parse the committer identity from the raw buffer.
	 * <p>
	 * This method parses and returns the content of the committer line, after
	 * taking the commit's character set into account and decoding the committer
	 * name and email address. This method is fairly expensive and produces a
	 * new PersonIdent instance on each invocation. Callers should invoke this
	 * method only if they are certain they will be outputting the result, and
	 * should cache the return value for as long as necessary to use all
	 * information from it.
	 * <p>
	 * RevFilter implementations should try to use {@link RawParseUtils} to scan
	 * the {@link #getRawBuffer()} instead, as this will allow faster evaluation
	 * of commits.
	 * 
	 * @return identity of the committer (name, email) and the time the commit
	 *         was made by the committer; null if no committer line was found.
	 */
	public final PersonIdent getCommitterIdent() {
		final byte[] raw = buffer;
		final int nameB = RawParseUtils.committer(raw, 0);
		if (nameB < 0)
			return null;
		return RawParseUtils.parsePersonIdent(raw, nameB);
	}

	/**
	 * Parse the complete commit message and decode it to a string.
	 * <p>
	 * This method parses and returns the message portion of the commit buffer,
	 * after taking the commit's character set into account and decoding the
	 * buffer using that character set. This method is a fairly expensive
	 * operation and produces a new string on each invocation.
	 * 
	 * @return decoded commit message as a string. Never null.
	 */
	public final String getFullMessage() {
		final byte[] raw = buffer;
		final int msgB = RawParseUtils.commitMessage(raw, 0);
		if (msgB < 0)
			return "";
		final Charset enc = RawParseUtils.parseEncoding(raw);
		return RawParseUtils.decode(enc, raw, msgB, raw.length);
	}

	/**
	 * Parse the commit message and return the first "line" of it.
	 * <p>
	 * The first line is everything up to the first pair of LFs. This is the
	 * "oneline" format, suitable for output in a single line display.
	 * <p>
	 * This method parses and returns the message portion of the commit buffer,
	 * after taking the commit's character set into account and decoding the
	 * buffer using that character set. This method is a fairly expensive
	 * operation and produces a new string on each invocation.
	 * 
	 * @return decoded commit message as a string. Never null. The returned
	 *         string does not contain any LFs, even if the first paragraph
	 *         spanned multiple lines. Embedded LFs are converted to spaces.
	 */
	public final String getShortMessage() {
		final byte[] raw = buffer;
		final int msgB = RawParseUtils.commitMessage(raw, 0);
		if (msgB < 0)
			return "";

		final Charset enc = RawParseUtils.parseEncoding(raw);
		final int msgE = RawParseUtils.endOfParagraph(raw, msgB);
		String str = RawParseUtils.decode(enc, raw, msgB, msgE);
		if (hasLF(raw, msgB, msgE))
			str = str.replace('\n', ' ');
		return str;
	}

	static boolean hasLF(final byte[] r, int b, final int e) {
		while (b < e)
			if (r[b++] == '\n')
				return true;
		return false;
	}

	/**
	 * Reset this commit to allow another RevWalk with the same instances.
	 * <p>
	 * Subclasses <b>must</b> call <code>super.reset()</code> to ensure the
	 * basic information can be correctly cleared out.
	 */
	public void reset() {
		inDegree = 0;
	}

	final void disposeBody() {
		buffer = null;
	}

	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder();
		s.append(Constants.typeString(getType()));
		s.append(' ');
		s.append(name());
		s.append(' ');
		s.append(commitTime);
		s.append(' ');
		appendCoreFlags(s);
		return s.toString();
	}
}
