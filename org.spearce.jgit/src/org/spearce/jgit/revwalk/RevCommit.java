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
import java.nio.charset.Charset;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.MutableObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.util.RawParseUtils;

/** A commit reference to a commit in the DAG. */
public class RevCommit extends RevObject {
	static final RevCommit[] NO_PARENTS = {};

	private static final String TYPE_COMMIT = Constants.TYPE_COMMIT;

	private RevTree tree;

	RevCommit[] parents;

	int commitTime;

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
	void parse(final RevWalk walk) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		final ObjectLoader ldr = walk.db.openObject(walk.curs, this);
		if (ldr == null)
			throw new MissingObjectException(this, TYPE_COMMIT);
		final byte[] data = ldr.getCachedBytes();
		if (Constants.OBJ_COMMIT != ldr.getType())
			throw new IncorrectObjectTypeException(this, TYPE_COMMIT);
		parseCanonical(walk, data);
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
			commitTime = RawParseUtils.parseBase10(raw, ptr, null);
		}

		buffer = raw;
		flags |= PARSED;
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
				p.flags |= carry;
				carryFlags(p, carry);
			}

			c = pList[0];
			c.flags |= carry;
		}
	}

	void carryFlags(final int carryMask) {
		final int carry = flags & carryMask;
		if (carry == 0)
			return;
		carryFlags(this, carry);
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
		carryFlags(flags & flag.mask);
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
	 *         was made by the comitter; null if no committer line was found.
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

	private static boolean hasLF(final byte[] r, int b, final int e) {
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

	public void dispose() {
		flags &= ~PARSED;
		buffer = null;
	}
}
