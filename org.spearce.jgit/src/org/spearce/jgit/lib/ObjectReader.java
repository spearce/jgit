/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

public abstract class ObjectReader {
    private ObjectId objectId;

    public ObjectId getId() throws IOException {
	if (objectId == null) {
	    final MessageDigest md = Constants.newMessageDigest();
	    final InputStream is = getInputStream();
	    try {
		final byte[] buf = new byte[2048];
		int r;
		md.update(Constants.encodeASCII(getType()));
		md.update((byte) ' ');
		md.update(Constants.encodeASCII(getSize()));
		md.update((byte) 0);
		while ((r = is.read(buf)) > 0) {
		    md.update(buf, 0, r);
		}
	    } finally {
		is.close();
	    }
	    objectId = new ObjectId(md.digest());
	}
	return objectId;
    }

    protected void setId(final ObjectId id) {
	if (objectId != null) {
	    throw new IllegalStateException("Id already set.");
	}
	objectId = id;
    }

    public BufferedReader getBufferedReader()
	    throws UnsupportedEncodingException, IOException {
	return new BufferedReader(new InputStreamReader(getInputStream(),
		Constants.CHARACTER_ENCODING));
    }

    public abstract String getType() throws IOException;

    public abstract long getSize() throws IOException;

    public abstract InputStream getInputStream() throws IOException;

    public abstract void close() throws IOException;
}
