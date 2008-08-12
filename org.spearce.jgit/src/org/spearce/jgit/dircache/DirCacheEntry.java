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

package org.spearce.jgit.dircache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.util.NB;

/**
 * A single file (or stage of a file) in a {@link DirCache}.
 * <p>
 * An entry represents exactly one stage of a file. If a file path is unmerged
 * then multiple DirCacheEntry instances may appear for the same path name.
 */
public class DirCacheEntry {
	// private static final int P_CTIME = 0;

	// private static final int P_CTIME_NSEC = 4;

	private static final int P_MTIME = 8;

	// private static final int P_MTIME_NSEC = 12;

	// private static final int P_DEV = 16;

	// private static final int P_INO = 20;

	private static final int P_MODE = 24;

	// private static final int P_UID = 28;

	// private static final int P_GID = 32;

	private static final int P_SIZE = 36;

	private static final int P_OBJECTID = 40;

	private static final int P_FLAGS = 60;

	static final int INFO_LEN = 62;

	private static final int ASSUME_VALID = 0x80;

	/** (Possibly shared) header information storage. */
	private final byte[] info;

	/** First location within {@link #info} where our header starts. */
	private final int infoOffset;

	/** Our encoded path name, from the root of the repository. */
	final byte[] path;

	DirCacheEntry(final byte[] sharedInfo, final int infoAt,
			final InputStream in) throws IOException {
		info = sharedInfo;
		infoOffset = infoAt;

		NB.readFully(in, info, infoOffset, INFO_LEN);

		int pathLen = NB.decodeUInt16(info, infoOffset + P_FLAGS) & 0xfff;
		path = new byte[pathLen];
		NB.readFully(in, path, 0, pathLen);

		// Index records are padded out to the next 8 byte alignment
		// for historical reasons related to how C Git read the files.
		//
		final int actLen = INFO_LEN + pathLen;
		final int expLen = (actLen + 8) & ~7;
		if (actLen != expLen)
			in.skip(expLen - actLen);
	}

	final byte[] idBuffer() {
		return info;
	}

	final int idOffset() {
		return infoOffset + P_OBJECTID;
	}

	/**
	 * Is this entry always thought to be unmodified?
	 * <p>
	 * Most entries in the index do not have this flag set. Users may however
	 * set them on if the file system stat() costs are too high on this working
	 * directory, such as on NFS or SMB volumes.
	 *
	 * @return true if we must assume the entry is unmodified.
	 */
	public boolean isAssumeValid() {
		return (info[infoOffset + P_FLAGS] & ASSUME_VALID) != 0;
	}

	/**
	 * Set the assume valid flag for this entry,
	 *
	 * @param assume
	 *            true to ignore apparent modifications; false to look at last
	 *            modified to detect file modifications.
	 */
	public void setAssumeValid(final boolean assume) {
		if (assume)
			info[infoOffset + P_FLAGS] |= ASSUME_VALID;
		else
			info[infoOffset + P_FLAGS] &= ~ASSUME_VALID;
	}

	/**
	 * Get the stage of this entry.
	 * <p>
	 * Entries have one of 4 possible stages: 0-3.
	 *
	 * @return the stage of this entry.
	 */
	public int getStage() {
		return (info[infoOffset + P_FLAGS] >>> 4) & 0x3;
	}

	/**
	 * Obtain the raw {@link FileMode} bits for this entry.
	 *
	 * @return mode bits for the entry.
	 * @see FileMode#fromBits(int)
	 */
	public int getRawMode() {
		return NB.decodeInt32(info, infoOffset + P_MODE);
	}

	/**
	 * Set the file mode for this entry.
	 *
	 * @param mode
	 *            the new mode constant.
	 */
	public void setFileMode(final FileMode mode) {
		NB.encodeInt32(info, infoOffset + P_MODE, mode.getBits());
	}

	/**
	 * Get the cached last modification date of this file, in milliseconds.
	 * <p>
	 * One of the indicators that the file has been modified by an application
	 * changing the working tree is if the last modification time for the file
	 * differs from the time stored in this entry.
	 *
	 * @return last modification time of this file, in milliseconds since the
	 *         Java epoch (midnight Jan 1, 1970 UTC).
	 */
	public long getLastModified() {
		return decodeTS(P_MTIME);
	}

	/**
	 * Set the cached last modification date of this file, using milliseconds.
	 *
	 * @param when
	 *            new cached modification date of the file, in milliseconds.
	 */
	public void setLastModified(final long when) {
		encodeTS(P_MTIME, when);
	}

	/**
	 * Get the cached size (in bytes) of this file.
	 * <p>
	 * One of the indicators that the file has been modified by an application
	 * changing the working tree is if the size of the file (in bytes) differs
	 * from the size stored in this entry.
	 * <p>
	 * Note that this is the length of the file in the working directory, which
	 * may differ from the size of the decompressed blob if work tree filters
	 * are being used, such as LF<->CRLF conversion.
	 *
	 * @return cached size of the working directory file, in bytes.
	 */
	public int getLength() {
		return NB.decodeInt32(info, infoOffset + P_SIZE);
	}

	/**
	 * Set the cached size (in bytes) of this file.
	 *
	 * @param sz
	 *            new cached size of the file, as bytes.
	 */
	public void setLength(final int sz) {
		NB.encodeInt32(info, infoOffset + P_SIZE, sz);
	}

	/**
	 * Obtain the ObjectId for the entry.
	 * <p>
	 * Using this method to compare ObjectId values between entries is
	 * inefficient as it causes memory allocation.
	 *
	 * @return object identifier for the entry.
	 */
	public ObjectId getObjectId() {
		return ObjectId.fromRaw(idBuffer(), idOffset());
	}

	/**
	 * Get the entry's complete path.
	 * <p>
	 * This method is not very efficient and is primarily meant for debugging
	 * and final output generation. Applications should try to avoid calling it,
	 * and if invoked do so only once per interesting entry, where the name is
	 * absolutely required for correct function.
	 *
	 * @return complete path of the entry, from the root of the repository. If
	 *         the entry is in a subtree there will be at least one '/' in the
	 *         returned string.
	 */
	public String getPathString() {
		return Constants.CHARSET.decode(ByteBuffer.wrap(path)).toString();
	}

	private long decodeTS(final int pIdx) {
		final int base = infoOffset + pIdx;
		final int sec = NB.decodeInt32(info, base);
		final int ms = NB.decodeInt32(info, base + 4) / 1000000;
		return 1000L * sec + ms;
	}

	private void encodeTS(final int pIdx, final long when) {
		final int base = infoOffset + pIdx;
		NB.encodeInt32(info, base, (int) (when / 1000));
		NB.encodeInt32(info, base + 4, ((int) (when % 1000)) * 1000000);
	}
}
