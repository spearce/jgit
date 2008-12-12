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

import static org.spearce.jgit.util.RawParseUtils.match;
import static org.spearce.jgit.util.RawParseUtils.nextLF;
import static org.spearce.jgit.util.RawParseUtils.parseBase10;

import org.spearce.jgit.util.MutableInteger;

/** Hunk header describing the layout of a single block of lines */
public class HunkHeader {
	private final FileHeader file;

	/** Offset within {@link #file}.buf to the "@@ -" line. */
	final int startOffset;

	/** Position 1 past the end of this hunk within {@link #file}'s buf. */
	int endOffset;

	/** First line number in the pre-image file where the hunk starts */
	int oldStartLine;

	/** Total number of pre-image lines this hunk covers (context + deleted) */
	int oldLineCount;

	/** First line number in the post-image file where the hunk starts */
	int newStartLine;

	/** Total number of post-image lines this hunk covers (context + inserted) */
	int newLineCount;

	/** Total number of lines of context appearing in this hunk */
	int nContext;

	/** Number of lines removed by this hunk */
	int nDeleted;

	/** Number of lines added by this hunk */
	int nAdded;

	HunkHeader(final FileHeader fh, final int offset) {
		file = fh;
		startOffset = offset;
	}

	/** @return header for the file this hunk applies to */
	public FileHeader getFileHeader() {
		return file;
	}

	/** @return first line number in the pre-image file where the hunk starts */
	public int getOldStartLine() {
		return oldStartLine;
	}

	/** @return total number of pre-image lines this hunk covers */
	public int getOldLineCount() {
		return oldLineCount;
	}

	/** @return first line number in the post-image file where the hunk starts */
	public int getNewStartLine() {
		return newStartLine;
	}

	/** @return Total number of post-image lines this hunk covers */
	public int getNewLineCount() {
		return newLineCount;
	}

	/** @return total number of lines of context appearing in this hunk */
	public int getLinesContext() {
		return nContext;
	}

	/** @return number of lines removed by this hunk */
	public int getLinesDeleted() {
		return nDeleted;
	}

	/** @return number of lines added by this hunk */
	public int getLinesAdded() {
		return nAdded;
	}

	void parseHeader() {
		// Parse "@@ -236,9 +236,9 @@ protected boolean"
		//
		final byte[] buf = file.buf;
		final MutableInteger ptr = new MutableInteger();
		ptr.value = nextLF(buf, startOffset, ' ');
		oldStartLine = -parseBase10(buf, ptr.value, ptr);
		oldLineCount = parseBase10(buf, ptr.value + 1, ptr);

		newStartLine = parseBase10(buf, ptr.value + 1, ptr);
		newLineCount = parseBase10(buf, ptr.value + 1, ptr);
	}

	int parseBody(final Patch script) {
		final byte[] buf = file.buf;
		final int sz = buf.length;
		int c = nextLF(buf, startOffset), last = c;

		nDeleted = 0;
		nAdded = 0;

		SCAN: for (; c < sz; last = c, c = nextLF(buf, c)) {
			switch (buf[c]) {
			case ' ':
			case '\n':
				nContext++;
				continue;

			case '-':
				nDeleted++;
				continue;

			case '+':
				nAdded++;
				continue;

			case '\\': // Matches "\ No newline at end of file"
				continue;

			default:
				break SCAN;
			}
		}

		if (last < sz && nContext + nDeleted - 1 == oldLineCount
				&& nContext + nAdded == newLineCount
				&& match(buf, last, Patch.SIG_FOOTER) >= 0) {
			// This is an extremely common occurrence of "corruption".
			// Users add footers with their signatures after this mark,
			// and git diff adds the git executable version number.
			// Let it slide; the hunk otherwise looked sound.
			//
			nDeleted--;
			return last;
		}

		if (nContext + nDeleted < oldLineCount) {
			final int missingCount = oldLineCount - (nContext + nDeleted);
			script.error(buf, startOffset, "Truncated hunk, at least "
					+ missingCount + " old lines is missing");

		} else if (nContext + nAdded < newLineCount) {
			final int missingCount = newLineCount - (nContext + nAdded);
			script.error(buf, startOffset, "Truncated hunk, at least "
					+ missingCount + " new lines is missing");

		} else if (nContext + nDeleted > oldLineCount
				|| nContext + nAdded > newLineCount) {
			script.warn(buf, startOffset, "Hunk header " + oldLineCount + ":"
					+ newLineCount + " does not match body line count of "
					+ (nContext + nDeleted) + ":" + (nContext + nAdded));
		}

		return c;
	}
}
