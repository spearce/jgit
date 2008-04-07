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
package org.spearce.jgit.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.spearce.jgit.util.NB;

/**
 * Access path to locate objects by {@link ObjectId} in a {@link PackFile}.
 * <p>
 * Indexes are strictly redundant information in that we can rebuild all of the
 * data held in the index file from the on disk representation of the pack file
 * itself, but it is faster to access for random requests because data is stored
 * by ObjectId.
 * </p>
 */
abstract class PackIndex {
	/**
	 * Open an existing pack <code>.idx</code> file for reading.
	 * <p>
	 * The format of the file will be automatically detected and a proper access
	 * implementation for that format will be constructed and returned to the
	 * caller. The file may or may not be held open by the returned instance.
	 * </p>
	 *
	 * @param idxFile
	 *            existing pack .idx to read.
	 * @return access implementation for the requested file.
	 * @throws FileNotFoundException
	 *             the file does not exist.
	 * @throws IOException
	 *             the file exists but could not be read due to security errors,
	 *             unrecognized data version, or unexpected data corruption.
	 */
	static PackIndex open(final File idxFile) throws IOException {
		final FileInputStream fd = new FileInputStream(idxFile);
		try {
			final byte[] hdr = new byte[8];
			NB.readFully(fd, hdr, 0, hdr.length);
			if (isTOC(hdr)) {
				final int v = NB.decodeInt32(hdr, 4);
				switch (v) {
				case 2:
					return new PackIndexV2(fd);
				default:
					throw new IOException("Unsupported pack index version " + v);
				}
			}
			return new PackIndexV1(fd, hdr);
		} catch (IOException ioe) {
			final String path = idxFile.getAbsolutePath();
			final IOException err;
			err = new IOException("Unreadable pack index: " + path);
			err.initCause(ioe);
			throw err;
		} finally {
			try {
				fd.close();
			} catch (IOException err2) {
				// ignore
			}
		}
	}

	private static boolean isTOC(final byte[] h) {
		return h[0] == -1 && h[1] == 't' && h[2] == 'O' && h[3] == 'c';
	}

	/**
	 * Obtain the total number of objects described by this index.
	 *
	 * @return number of objects in this index, and likewise in the associated
	 *         pack that this index was generated from.
	 */
	abstract long getObjectCount();

	/**
	 * Locate the file offset position for the requested object.
	 *
	 * @param objId
	 *            name of the object to locate within the pack.
	 * @return offset of the object's header and compressed content; -1 if the
	 *         object does not exist in this index and is thus not stored in the
	 *         associated pack.
	 */
	abstract long findOffset(AnyObjectId objId);
}
