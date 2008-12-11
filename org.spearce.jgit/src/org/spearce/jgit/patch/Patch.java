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
import static org.spearce.jgit.patch.FileHeader.HUNK_HDR;
import static org.spearce.jgit.patch.FileHeader.NEW_NAME;
import static org.spearce.jgit.patch.FileHeader.OLD_NAME;
import static org.spearce.jgit.util.RawParseUtils.match;
import static org.spearce.jgit.util.RawParseUtils.nextLF;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.spearce.jgit.util.TemporaryBuffer;

/** A parsed collection of {@link FileHeader}s from a unified diff patch file */
public class Patch {
	private static final byte[] DIFF_GIT = encodeASCII("diff --git ");

	private static final byte[] DIFF_CC = encodeASCII("diff --cc ");

	/** The files, in the order they were parsed out of the input. */
	private final List<FileHeader> files;

	/** Create an empty patch. */
	public Patch() {
		files = new ArrayList<FileHeader>();
	}

	/**
	 * Add a single file to this patch.
	 * <p>
	 * Typically files should be added by parsing the text through one of this
	 * class's parse methods.
	 *
	 * @param fh
	 *            the header of the file.
	 */
	public void addFile(final FileHeader fh) {
		files.add(fh);
	}

	/** @return list of files described in the patch, in occurrence order. */
	public List<FileHeader> getFiles() {
		return files;
	}

	/**
	 * Parse a patch received from an InputStream.
	 * <p>
	 * Multiple parse calls on the same instance will concatenate the patch
	 * data, but each parse input must start with a valid file header (don't
	 * split a single file across parse calls).
	 *
	 * @param is
	 *            the stream to read the patch data from. The stream is read
	 *            until EOF is reached.
	 * @throws IOException
	 *             there was an error reading from the input stream.
	 */
	public void parse(final InputStream is) throws IOException {
		final byte[] buf = readFully(is);
		parse(buf, 0, buf.length);
	}

	private static byte[] readFully(final InputStream is) throws IOException {
		final TemporaryBuffer b = new TemporaryBuffer();
		b.copy(is);
		final byte[] buf = b.toByteArray();
		return buf;
	}

	/**
	 * Parse a patch stored in a byte[].
	 * <p>
	 * Multiple parse calls on the same instance will concatenate the patch
	 * data, but each parse input must start with a valid file header (don't
	 * split a single file across parse calls).
	 *
	 * @param buf
	 *            the buffer to parse.
	 * @param ptr
	 *            starting position to parse from.
	 * @param end
	 *            1 past the last position to end parsing. The total length to
	 *            be parsed is <code>end - ptr</code>.
	 */
	public void parse(final byte[] buf, int ptr, final int end) {
		while (ptr < end)
			ptr = parseFile(buf, ptr);
	}

	private int parseFile(final byte[] buf, int c) {
		final int sz = buf.length;
		while (c < sz) {
			// Valid git style patch?
			//
			if (match(buf, c, DIFF_GIT) >= 0)
				return parseDiffGit(buf, c);
			if (match(buf, c, DIFF_CC) >= 0)
				return parseDiffCC(buf, c);

			// Junk between files? Leading junk? Traditional
			// (non-git generated) patch?
			//
			final int n = nextLF(buf, c);
			if (n >= sz) {
				// Patches cannot be only one line long. This must be
				// trailing junk that we should ignore.
				//
				return sz;
			}

			if (n - c < 6) {
				// A valid header must be at least 6 bytes on the
				// first line, e.g. "--- a/b\n".
				//
				c = n;
				continue;
			}

			if (match(buf, c, OLD_NAME) >= 0 && match(buf, n, NEW_NAME) >= 0) {
				// Probably a traditional patch. Ensure we have at least
				// a "@@ -0,0" smelling line next. We only check the "@@ -".
				//
				final int f = nextLF(buf, n);
				if (f >= sz)
					return sz;
				if (match(buf, f, HUNK_HDR) >= 0)
					return parseTraditionalPatch(buf, c);
			}

			c = n;
		}
		return c;
	}

	private int parseDiffGit(final byte[] buf, final int startOffset) {
		final FileHeader fh = new FileHeader(buf, startOffset);
		int ptr = fh.parseGitFileName(startOffset + DIFF_GIT.length);
		if (ptr < 0)
			return skipFile(buf, startOffset);

		ptr = fh.parseGitHeaders(ptr);
		// TODO parse hunks
		fh.endOffset = ptr;
		addFile(fh);
		return ptr;
	}

	private int parseDiffCC(final byte[] buf, final int startOffset) {
		final FileHeader fh = new FileHeader(buf, startOffset);
		int ptr = fh.parseGitFileName(startOffset + DIFF_CC.length);
		if (ptr < 0)
			return skipFile(buf, startOffset);

		// TODO Support parsing diff --cc headers
		// TODO parse diff --cc hunks
		fh.endOffset = ptr;
		addFile(fh);
		return ptr;
	}

	private int parseTraditionalPatch(final byte[] buf, final int startOffset) {
		final FileHeader fh = new FileHeader(buf, startOffset);
		int ptr = fh.parseTraditionalHeaders(startOffset);
		// TODO parse hunks
		fh.endOffset = ptr;
		addFile(fh);
		return ptr;
	}

	private static int skipFile(final byte[] buf, int ptr) {
		ptr = nextLF(buf, ptr);
		if (match(buf, ptr, OLD_NAME) >= 0)
			ptr = nextLF(buf, ptr);
		return ptr;
	}
}
