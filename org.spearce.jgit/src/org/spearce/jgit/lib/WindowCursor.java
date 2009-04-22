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

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Active handle to a ByteWindow. */
public final class WindowCursor {
	/** Temporary buffer large enough for at least one raw object id. */
	final byte[] tempId = new byte[Constants.OBJECT_ID_LENGTH];

	private Inflater inf;

	ByteWindow window;

	Object handle;

	/**
	 * Copy bytes from the window to a caller supplied buffer.
	 * 
	 * @param provider
	 *            the file the desired window is stored within.
	 * @param position
	 *            position within the file to read from.
	 * @param dstbuf
	 *            destination buffer to copy into.
	 * @param dstoff
	 *            offset within <code>dstbuf</code> to start copying into.
	 * @param cnt
	 *            number of bytes to copy. This value may exceed the number of
	 *            bytes remaining in the window starting at offset
	 *            <code>pos</code>.
	 * @return number of bytes actually copied; this may be less than
	 *         <code>cnt</code> if <code>cnt</code> exceeded the number of
	 *         bytes available.
	 * @throws IOException
	 *             this cursor does not match the provider or id and the proper
	 *             window could not be acquired through the provider's cache.
	 */
	int copy(final PackFile provider, long position, final byte[] dstbuf,
			int dstoff, final int cnt) throws IOException {
		final long length = provider.length;
		int need = cnt;
		while (need > 0 && position < length) {
			pin(provider, position);
			final int r = window.copy(handle, position, dstbuf, dstoff, need);
			position += r;
			dstoff += r;
			need -= r;
		}
		return cnt - need;
	}

	/**
	 * Pump bytes into the supplied inflater as input.
	 * 
	 * @param provider
	 *            the file the desired window is stored within.
	 * @param position
	 *            position within the file to read from.
	 * @param dstbuf
	 *            destination buffer the inflater should output decompressed
	 *            data to.
	 * @param dstoff
	 *            current offset within <code>dstbuf</code> to inflate into.
	 * @return updated <code>dstoff</code> based on the number of bytes
	 *         successfully inflated into <code>dstbuf</code>.
	 * @throws IOException
	 *             this cursor does not match the provider or id and the proper
	 *             window could not be acquired through the provider's cache.
	 * @throws DataFormatException
	 *             the inflater encountered an invalid chunk of data. Data
	 *             stream corruption is likely.
	 */
	int inflate(final PackFile provider, long position, final byte[] dstbuf,
			int dstoff) throws IOException, DataFormatException {
		if (inf == null)
			inf = InflaterCache.get();
		else
			inf.reset();
		for (;;) {
			pin(provider, position);
			dstoff = window.inflate(handle, position, dstbuf, dstoff, inf);
			if (inf.finished())
				return dstoff;
			position = window.end;
		}
	}

	void inflateVerify(final PackFile provider, long position)
			throws IOException, DataFormatException {
		if (inf == null)
			inf = InflaterCache.get();
		else
			inf.reset();
		for (;;) {
			pin(provider, position);
			window.inflateVerify(handle, position, inf);
			if (inf.finished())
				return;
			position = window.end;
		}
	}

	private void pin(final PackFile provider, final long position)
			throws IOException {
		final ByteWindow w = window;
		if (w == null || !w.contains(provider, position))
			WindowCache.get(this, provider, position);
	}

	/** Release the current window cursor. */
	public void release() {
		window = null;
		handle = null;
		try {
			InflaterCache.release(inf);
		} finally {
			inf = null;
		}
	}

	/**
	 * @param curs cursor to release; may be null.
	 * @return always null.
	 */
	public static WindowCursor release(final WindowCursor curs) {
		if (curs != null)
			curs.release();
		return null;
	}
}
