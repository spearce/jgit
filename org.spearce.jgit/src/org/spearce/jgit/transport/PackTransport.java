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
package org.spearce.jgit.transport;

import org.spearce.jgit.lib.Repository;

/**
 * Canonical implementation of an object transport using Git pack transfers.
 * <p>
 * Implementations of PackTransport setup connections and move objects back and
 * forth by creating pack files on the source side and indexing them on the
 * receiving side.
 * 
 * @see PackFetchConnection
 */
abstract class PackTransport extends Transport {
	PackTransport(final Repository local, final URIish u) {
		super(local, u);
	}
}
