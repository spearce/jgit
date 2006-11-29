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

import java.nio.ByteBuffer;

public final class ByteBufferWindow extends ByteWindow {
    private final ByteBuffer buffer;

    public ByteBufferWindow(final WindowProvider o, final int d,
	    final ByteBuffer b) {
	super(o, d);
	buffer = b;
    }

    public final int copy(final int p, final byte[] b, final int o, int n) {
	final ByteBuffer s = buffer.slice();
	s.position(p);
	n = Math.min(s.remaining(), n);
	s.get(b, o, n);
	return n;
    }

    public int size() {
	return buffer.capacity();
    }
}