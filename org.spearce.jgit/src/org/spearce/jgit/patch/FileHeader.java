/*
 * Copyright (C) 2008, Google Inc.
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

package org.spearce.jgit.patch;

import static org.spearce.jgit.lib.Constants.encodeASCII;
import static org.spearce.jgit.util.RawParseUtils.decode;
import static org.spearce.jgit.util.RawParseUtils.match;
import static org.spearce.jgit.util.RawParseUtils.nextLF;
import static org.spearce.jgit.util.RawParseUtils.parseBase10;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.spearce.jgit.lib.AbbreviatedObjectId;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.util.QuotedString;

/** Patch header describing an action for a single file path. */
public class FileHeader {
	/** Magical file name used for file adds or deletes. */
	public static final String DEV_NULL = "/dev/null";

	private static final byte[] OLD_MODE = encodeASCII("old mode ");

	private static final byte[] NEW_MODE = encodeASCII("new mode ");

	private static final byte[] DELETED_FILE_MODE = encodeASCII("deleted file mode ");

	private static final byte[] NEW_FILE_MODE = encodeASCII("new file mode ");

	private static final byte[] COPY_FROM = encodeASCII("copy from ");

	private static final byte[] COPY_TO = encodeASCII("copy to ");

	private static final byte[] RENAME_OLD = encodeASCII("rename old ");

	private static final byte[] RENAME_NEW = encodeASCII("rename new ");

	private static final byte[] RENAME_FROM = encodeASCII("rename from ");

	private static final byte[] RENAME_TO = encodeASCII("rename to ");

	private static final byte[] SIMILARITY_INDEX = encodeASCII("similarity index ");

	private static final byte[] DISSIMILARITY_INDEX = encodeASCII("dissimilarity index ");

	private static final byte[] INDEX = encodeASCII("index ");

	static final byte[] OLD_NAME = encodeASCII("--- ");

	static final byte[] NEW_NAME = encodeASCII("+++ ");

	/** General type of change a single file-level patch describes. */
	public static enum ChangeType {
		/** Add a new file to the project */
		ADD,

		/** Modify an existing file in the project (content and/or mode) */
		MODIFY,

		/** Delete an existing file from the project */
		DELETE,

		/** Rename an existing file to a new location */
		RENAME,

		/** Copy an existing file to a new location, keeping the original */
		COPY;
	}

	/** Type of patch used by this file. */
	public static enum PatchType {
		/** A traditional unified diff style patch of a text file. */
		UNIFIED,

		/** An empty patch with a message "Binary files ... differ" */
		BINARY,

		/** A Git binary patch, holding pre and post image deltas */
		GIT_BINARY;
	}

	/** Buffer holding the patch data for this file. */
	final byte[] buf;

	/** Offset within {@link #buf} to the "diff ..." line. */
	final int startOffset;

	/** Position 1 past the end of this file within {@link #buf}. */
	int endOffset;

	/** File name of the old (pre-image). */
	private String oldName;

	/** File name of the new (post-image). */
	private String newName;

	/** Old mode of the file, if described by the patch, else null. */
	private FileMode oldMode;

	/** New mode of the file, if described by the patch, else null. */
	private FileMode newMode;

	/** General type of change indicated by the patch. */
	private ChangeType changeType;

	/** Similarity score if {@link #changeType} is a copy or rename. */
	private int score;

	/** ObjectId listed on the index line for the old (pre-image) */
	private AbbreviatedObjectId oldId;

	/** ObjectId listed on the index line for the new (post-image) */
	private AbbreviatedObjectId newId;

	/** Type of patch used to modify this file */
	PatchType patchType;

	/** The hunks of this file */
	private List<HunkHeader> hunks;

	/** If {@link #patchType} is {@link PatchType#GIT_BINARY}, the new image */
	BinaryHunk forwardBinaryHunk;

	/** If {@link #patchType} is {@link PatchType#GIT_BINARY}, the old image */
	BinaryHunk reverseBinaryHunk;

	FileHeader(final byte[] b, final int offset) {
		buf = b;
		startOffset = offset;
		changeType = ChangeType.MODIFY; // unless otherwise designated
		patchType = PatchType.UNIFIED;
	}

	/**
	 * Get the old name associated with this file.
	 * <p>
	 * The meaning of the old name can differ depending on the semantic meaning
	 * of this patch:
	 * <ul>
	 * <li><i>file add</i>: always <code>/dev/null</code></li>
	 * <li><i>file modify</i>: always {@link #getNewName()}</li>
	 * <li><i>file delete</i>: always the file being deleted</li>
	 * <li><i>file copy</i>: source file the copy originates from</li>
	 * <li><i>file rename</i>: source file the rename originates from</li>
	 * </ul>
	 *
	 * @return old name for this file.
	 */
	public String getOldName() {
		return oldName;
	}

	/**
	 * Get the new name associated with this file.
	 * <p>
	 * The meaning of the new name can differ depending on the semantic meaning
	 * of this patch:
	 * <ul>
	 * <li><i>file add</i>: always the file being created</li>
	 * <li><i>file modify</i>: always {@link #getOldName()}</li>
	 * <li><i>file delete</i>: always <code>/dev/null</code></li>
	 * <li><i>file copy</i>: destination file the copy ends up at</li>
	 * <li><i>file rename</i>: destination file the rename ends up at/li>
	 * </ul>
	 *
	 * @return new name for this file.
	 */
	public String getNewName() {
		return newName;
	}

	/** @return the old file mode, if described in the patch */
	public FileMode getOldMode() {
		return oldMode;
	}

	/** @return the new file mode, if described in the patch */
	public FileMode getNewMode() {
		return newMode;
	}

	/** @return the type of change this patch makes on {@link #getNewName()} */
	public ChangeType getChangeType() {
		return changeType;
	}

	/**
	 * @return similarity score between {@link #getOldName()} and
	 *         {@link #getNewName()} if {@link #getChangeType()} is
	 *         {@link ChangeType#COPY} or {@link ChangeType#RENAME}.
	 */
	public int getScore() {
		return score;
	}

	/**
	 * Get the old object id from the <code>index</code>.
	 *
	 * @return the object id; null if there is no index line
	 */
	public AbbreviatedObjectId getOldId() {
		return oldId;
	}

	/**
	 * Get the new object id from the <code>index</code>.
	 *
	 * @return the object id; null if there is no index line
	 */
	public AbbreviatedObjectId getNewId() {
		return newId;
	}

	/** @return style of patch used to modify this file */
	public PatchType getPatchType() {
		return patchType;
	}

	/** @return true if this patch modifies metadata about a file */
	public boolean hasMetaDataChanges() {
		return changeType != ChangeType.MODIFY || newMode != oldMode;
	}

	/** @return hunks altering this file; in order of appearance in patch */
	public List<HunkHeader> getHunks() {
		if (hunks == null)
			return Collections.emptyList();
		return hunks;
	}

	void addHunk(final HunkHeader h) {
		if (h.getFileHeader() != this)
			throw new IllegalArgumentException("Hunk belongs to another file");
		if (hunks == null)
			hunks = new ArrayList<HunkHeader>();
		hunks.add(h);
	}

	/** @return if a {@link PatchType#GIT_BINARY}, the new-image delta/literal */
	public BinaryHunk getForwardBinaryHunk() {
		return forwardBinaryHunk;
	}

	/** @return if a {@link PatchType#GIT_BINARY}, the old-image delta/literal */
	public BinaryHunk getReverseBinaryHunk() {
		return reverseBinaryHunk;
	}

	/**
	 * Parse a "diff --git" or "diff --cc" line.
	 *
	 * @param ptr
	 *            first character after the "diff --git " or "diff --cc " part.
	 * @param end
	 *            one past the last position to parse.
	 * @return first character after the LF at the end of the line; -1 on error.
	 */
	int parseGitFileName(int ptr, final int end) {
		final int eol = nextLF(buf, ptr);
		final int bol = ptr;
		if (eol >= end) {
			return -1;
		}

		// buffer[ptr..eol] looks like "a/foo b/foo\n". After the first
		// A regex to match this is "^[^/]+/(.*?) [^/+]+/\1\n$". There
		// is only one way to split the line such that text to the left
		// of the space matches the text to the right, excluding the part
		// before the first slash.
		//

		final int aStart = nextLF(buf, ptr, '/');
		if (aStart >= eol)
			return eol;

		while (ptr < eol) {
			final int sp = nextLF(buf, ptr, ' ');
			if (sp >= eol) {
				// We can't split the header, it isn't valid.
				// This may be OK if this is a rename patch.
				//
				return eol;
			}
			final int bStart = nextLF(buf, sp, '/');
			if (bStart >= eol)
				return eol;

			// If buffer[aStart..sp - 1] = buffer[bStart..eol - 1]
			// we have a valid split.
			//
			if (eq(aStart, sp - 1, bStart, eol - 1)) {
				if (buf[bol] == '"') {
					// We're a double quoted name. The region better end
					// in a double quote too, and we need to decode the
					// characters before reading the name.
					//
					if (buf[sp - 2] != '"') {
						return eol;
					}
					oldName = QuotedString.GIT_PATH.dequote(buf, bol, sp - 1);
					oldName = p1(oldName);
				} else {
					oldName = decode(Constants.CHARSET, buf, aStart, sp - 1);
				}
				newName = oldName;
				return eol;
			}

			// This split wasn't correct. Move past the space and try
			// another split as the space must be part of the file name.
			//
			ptr = sp;
		}

		return eol;
	}

	int parseGitHeaders(int ptr, final int end) {
		while (ptr < end) {
			final int eol = nextLF(buf, ptr);
			if (isHunkHdr(buf, ptr, eol) >= 1) {
				// First hunk header; break out and parse them later.
				break;

			} else if (match(buf, ptr, OLD_NAME) >= 0) {
				oldName = p1(parseName(oldName, ptr + OLD_NAME.length, eol));
				if (oldName == DEV_NULL)
					changeType = ChangeType.ADD;

			} else if (match(buf, ptr, NEW_NAME) >= 0) {
				newName = p1(parseName(newName, ptr + NEW_NAME.length, eol));
				if (newName == DEV_NULL)
					changeType = ChangeType.DELETE;

			} else if (match(buf, ptr, OLD_MODE) >= 0) {
				oldMode = parseFileMode(ptr + OLD_MODE.length, eol);

			} else if (match(buf, ptr, NEW_MODE) >= 0) {
				newMode = parseFileMode(ptr + NEW_MODE.length, eol);

			} else if (match(buf, ptr, DELETED_FILE_MODE) >= 0) {
				oldMode = parseFileMode(ptr + DELETED_FILE_MODE.length, eol);
				changeType = ChangeType.DELETE;

			} else if (match(buf, ptr, NEW_FILE_MODE) >= 0) {
				newMode = parseFileMode(ptr + NEW_FILE_MODE.length, eol);
				changeType = ChangeType.ADD;

			} else if (match(buf, ptr, COPY_FROM) >= 0) {
				oldName = parseName(oldName, ptr + COPY_FROM.length, eol);
				changeType = ChangeType.COPY;

			} else if (match(buf, ptr, COPY_TO) >= 0) {
				newName = parseName(newName, ptr + COPY_TO.length, eol);
				changeType = ChangeType.COPY;

			} else if (match(buf, ptr, RENAME_OLD) >= 0) {
				oldName = parseName(oldName, ptr + RENAME_OLD.length, eol);
				changeType = ChangeType.RENAME;

			} else if (match(buf, ptr, RENAME_NEW) >= 0) {
				newName = parseName(newName, ptr + RENAME_NEW.length, eol);
				changeType = ChangeType.RENAME;

			} else if (match(buf, ptr, RENAME_FROM) >= 0) {
				oldName = parseName(oldName, ptr + RENAME_FROM.length, eol);
				changeType = ChangeType.RENAME;

			} else if (match(buf, ptr, RENAME_TO) >= 0) {
				newName = parseName(newName, ptr + RENAME_TO.length, eol);
				changeType = ChangeType.RENAME;

			} else if (match(buf, ptr, SIMILARITY_INDEX) >= 0) {
				score = parseBase10(buf, ptr + SIMILARITY_INDEX.length, null);

			} else if (match(buf, ptr, DISSIMILARITY_INDEX) >= 0) {
				score = parseBase10(buf, ptr + DISSIMILARITY_INDEX.length, null);

			} else if (match(buf, ptr, INDEX) >= 0) {
				parseIndexLine(ptr + INDEX.length, eol);

			} else {
				// Probably an empty patch (stat dirty).
				break;
			}

			ptr = eol;
		}
		return ptr;
	}

	int parseTraditionalHeaders(int ptr, final int end) {
		while (ptr < end) {
			final int eol = nextLF(buf, ptr);
			if (isHunkHdr(buf, ptr, eol) >= 1) {
				// First hunk header; break out and parse them later.
				break;

			} else if (match(buf, ptr, OLD_NAME) >= 0) {
				oldName = p1(parseName(oldName, ptr + OLD_NAME.length, eol));
				if (oldName == DEV_NULL)
					changeType = ChangeType.ADD;

			} else if (match(buf, ptr, NEW_NAME) >= 0) {
				newName = p1(parseName(newName, ptr + NEW_NAME.length, eol));
				if (newName == DEV_NULL)
					changeType = ChangeType.DELETE;

			} else {
				// Possibly an empty patch.
				break;
			}

			ptr = eol;
		}
		return ptr;
	}

	private String parseName(final String expect, int ptr, final int end) {
		if (ptr == end)
			return expect;

		String r;
		if (buf[ptr] == '"') {
			// New style GNU diff format
			//
			r = QuotedString.GIT_PATH.dequote(buf, ptr, end - 1);
		} else {
			// Older style GNU diff format, an optional tab ends the name.
			//
			int tab = end;
			while (ptr < tab && buf[tab - 1] != '\t')
				tab--;
			if (ptr == tab)
				tab = end;
			r = decode(Constants.CHARSET, buf, ptr, tab - 1);
		}

		if (r.equals(DEV_NULL))
			r = DEV_NULL;
		return r;
	}

	private static String p1(final String r) {
		final int s = r.indexOf('/');
		return s > 0 ? r.substring(s + 1) : r;
	}

	private FileMode parseFileMode(int ptr, final int end) {
		int tmp = 0;
		while (ptr < end - 1) {
			tmp <<= 3;
			tmp += buf[ptr++] - '0';
		}
		return FileMode.fromBits(tmp);
	}

	private void parseIndexLine(int ptr, final int end) {
		// "index $asha1..$bsha1[ $mode]" where $asha1 and $bsha1
		// can be unique abbreviations
		//
		final int dot2 = nextLF(buf, ptr, '.');
		final int mode = nextLF(buf, dot2, ' ');

		oldId = AbbreviatedObjectId.fromString(buf, ptr, dot2 - 1);
		newId = AbbreviatedObjectId.fromString(buf, dot2 + 1, mode - 1);

		if (mode < end)
			newMode = oldMode = parseFileMode(mode, end);
	}

	private boolean eq(int aPtr, int aEnd, int bPtr, int bEnd) {
		if (aEnd - aPtr != bEnd - bPtr) {
			return false;
		}
		while (aPtr < aEnd) {
			if (buf[aPtr++] != buf[bPtr++])
				return false;
		}
		return true;
	}

	/**
	 * Determine if this is a patch hunk header.
	 *
	 * @param buf
	 *            the buffer to scan
	 * @param start
	 *            first position in the buffer to evaluate
	 * @param end
	 *            last position to consider; usually the end of the buffer (
	 *            <code>buf.length</code>) or the first position on the next
	 *            line. This is only used to avoid very long runs of '@' from
	 *            killing the scan loop.
	 * @return the number of "ancestor revisions" in the hunk header. A
	 *         traditional two-way diff ("@@ -...") returns 1; a combined diff
	 *         for a 3 way-merge returns 3. If this is not a hunk header, 0 is
	 *         returned instead.
	 */
	static int isHunkHdr(final byte[] buf, final int start, final int end) {
		int ptr = start;
		while (ptr < end && buf[ptr] == '@')
			ptr++;
		if (ptr - start < 2)
			return 0;
		if (ptr == end || buf[ptr++] != ' ')
			return 0;
		if (ptr == end || buf[ptr++] != '-')
			return 0;
		return (ptr - 3) - start;
	}
}
