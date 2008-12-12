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

	private static final byte[][] BIN_HEADERS = new byte[][] {
			encodeASCII("Binary files "), encodeASCII("Files "), };

	private static final byte[] BIN_TRAILER = encodeASCII(" differ\n");

	private static final byte[] GIT_BINARY = encodeASCII("GIT binary patch\n");

	static final byte[] SIG_FOOTER = encodeASCII("-- \n");

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
		try {
			b.copy(is);
			b.close();
			return b.toByteArray();
		} finally {
			b.destroy();
		}
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
			if (match(buf, c, HUNK_HDR) >= 0) {
				// If we find a disconnected hunk header we might
				// have missed a file header previously. The hunk
				// isn't valid without knowing where it comes from.
				//

				// TODO handle a disconnected hunk fragment
				c = nextLF(buf, c);
				continue;
			}

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
		ptr = parseHunks(fh, ptr);
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
		ptr = parseHunks(fh, ptr);
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

	private int parseHunks(final FileHeader fh, int c) {
		final byte[] buf = fh.buf;
		final int sz = buf.length;
		while (c < sz) {
			// If we see a file header at this point, we have all of the
			// hunks for our current file. We should stop and report back
			// with this position so it can be parsed again later.
			//
			if (match(buf, c, DIFF_GIT) >= 0)
				break;
			if (match(buf, c, DIFF_CC) >= 0)
				break;
			if (match(buf, c, OLD_NAME) >= 0)
				break;
			if (match(buf, c, NEW_NAME) >= 0)
				break;

			if (match(buf, c, HUNK_HDR) >= 0) {
				final HunkHeader h = new HunkHeader(fh, c);
				h.parseHeader();
				c = h.parseBody();
				h.endOffset = c;
				fh.addHunk(h);
				if (c < sz && buf[c] != '@' && buf[c] != 'd'
						&& match(buf, c, SIG_FOOTER) < 0) {
					// TODO report on noise between hunks, might be an error
				}
				continue;
			}

			final int eol = nextLF(buf, c);
			if (fh.getHunks().isEmpty() && match(buf, c, GIT_BINARY) >= 0) {
				fh.patchType = FileHeader.PatchType.GIT_BINARY;
				return parseGitBinary(fh, eol);
			}

			if (fh.getHunks().isEmpty() && BIN_TRAILER.length < eol - c
					&& match(buf, eol - BIN_TRAILER.length, BIN_TRAILER) >= 0
					&& matchAny(buf, c, BIN_HEADERS)) {
				// The patch is a binary file diff, with no deltas.
				//
				fh.patchType = FileHeader.PatchType.BINARY;
				return eol;
			}

			// Skip this line and move to the next. Its probably garbage
			// after the last hunk of a file.
			//
			c = eol;
		}

		if (fh.getHunks().isEmpty()
				&& fh.getPatchType() == FileHeader.PatchType.UNIFIED
				&& !fh.hasMetaDataChanges()) {
			// Hmm, an empty patch? If there is no metadata here we
			// really have a binary patch that we didn't notice above.
			//
			fh.patchType = FileHeader.PatchType.BINARY;
		}

		return c;
	}

	private int parseGitBinary(final FileHeader fh, int c) {
		final BinaryHunk postImage = new BinaryHunk(fh, c);
		final int nEnd = postImage.parseHunk(c);
		if (nEnd < 0) {
			// Not a binary hunk.
			//

			// TODO handle invalid binary hunks
			return c;
		}
		c = nEnd;
		postImage.endOffset = c;
		fh.forwardBinaryHunk = postImage;

		final BinaryHunk preImage = new BinaryHunk(fh, c);
		final int oEnd = preImage.parseHunk(c);
		if (oEnd >= 0) {
			c = oEnd;
			preImage.endOffset = c;
			fh.reverseBinaryHunk = preImage;
		}

		return c;
	}

	private static boolean matchAny(final byte[] buf, final int c,
			final byte[][] srcs) {
		for (final byte[] s : srcs) {
			if (match(buf, c, s) >= 0)
				return true;
		}
		return false;
	}
}
