/*
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A {@link ByteWindow} with an underlying byte array for storage.
 */
final class ByteArrayWindow extends ByteWindow<byte[]> {
	boolean loaded;

	/**
	 * Constructor for ByteWindow.
	 * 
	 * @param o
	 *            the PackFile providing data access
	 * @param p
	 *            the file offset.
	 * @param d
	 *            an id provided by the PackFile. See
	 *            {@link WindowCache#get(WindowCursor, PackFile, long)}.
	 * @param b
	 *            byte array for storage
	 */
	ByteArrayWindow(final PackFile o, final long p, final int d, final byte[] b) {
		super(o, p, d, b, b.length);
	}

	int copy(final byte[] array, final int p, final byte[] b, final int o, int n) {
		n = Math.min(array.length - p, n);
		System.arraycopy(array, p, b, o, n);
		return n;
	}

	int inflate(final byte[] array, final int pos, final byte[] b, int o,
			final Inflater inf) throws DataFormatException {
		while (!inf.finished()) {
			if (inf.needsInput()) {
				inf.setInput(array, pos, array.length - pos);
				break;
			}
			o += inf.inflate(b, o, b.length - o);
		}
		while (!inf.finished() && !inf.needsInput())
			o += inf.inflate(b, o, b.length - o);
		return o;
	}

	void inflateVerify(final byte[] array, final int pos, final Inflater inf)
			throws DataFormatException {
		while (!inf.finished()) {
			if (inf.needsInput()) {
				inf.setInput(array, pos, array.length - pos);
				break;
			}
			inf.inflate(verifyGarbageBuffer, 0, verifyGarbageBuffer.length);
		}
		while (!inf.finished() && !inf.needsInput())
			inf.inflate(verifyGarbageBuffer, 0, verifyGarbageBuffer.length);
	}

	void ensureLoaded(final byte[] array) {
		boolean release = false;
		try {
			synchronized (this) {
				if (!loaded) {
					release = true;
					try {
						provider.fd.getChannel().read(ByteBuffer.wrap(array),
								start);
					} catch (IOException e) {
						throw new RuntimeException("Cannot fault in window", e);
					}
					loaded = true;
				}
			}
		} finally {
			if (release) {
				WindowCache.markLoaded(this);
			}
		}
	}
}