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
import java.io.OutputStream;

/**
 * Base class for a set of object loader classes for packed objects.
 */
abstract class PackedObjectLoader extends ObjectLoader {
	protected final PackFile pack;

	protected final WindowCursor curs;

	protected final long dataOffset;

	protected final long objectOffset;

	protected int objectType;

	protected int objectSize;

	PackedObjectLoader(final WindowCursor c, final PackFile pr,
			final long dataOffset, final long objectOffset) {
		curs = c;
		pack = pr;
		this.dataOffset = dataOffset;
		this.objectOffset = objectOffset;
	}

	public int getType() throws IOException {
		return objectType;
	}

	public long getSize() throws IOException {
		return objectSize;
	}

	/**
	 * @return offset of object data within pack file
	 */
	public long getDataOffset() {
		return dataOffset;
	}

	/**
	 * Copy raw object representation from storage to provided output stream.
	 * <p>
	 * Copied data doesn't include object header. User must provide temporary
	 * buffer used during copying by underlying I/O layer.
	 * </p>
	 *
	 * @param out
	 *            output stream when data is copied. No buffering is guaranteed.
	 * @param buf
	 *            temporary buffer used during copying. Recommended size is at
	 *            least few kB.
	 * @throws IOException
	 *             when the object cannot be read.
	 */
	public void copyRawData(OutputStream out, byte buf[]) throws IOException {
		pack.copyRawData(this, out, buf);
	}

	/**
	 * @return true if this loader is capable of fast raw-data copying basing on
	 *         compressed data checksum; false if raw-data copying needs
	 *         uncompressing and compressing data
	 * @throws IOException
	 *             the index file format cannot be determined.
	 */
	public boolean supportsFastCopyRawData() throws IOException {
		return pack.supportsFastCopyRawData();
	}

	/**
	 * @return id of delta base object for this object representation. null if
	 *         object is not stored as delta.
	 * @throws IOException
	 *             when delta base cannot read.
	 */
	public abstract ObjectId getDeltaBase() throws IOException;
}
