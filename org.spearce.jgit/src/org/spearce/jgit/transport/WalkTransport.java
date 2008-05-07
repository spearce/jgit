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
 * Canonical implementation of an object transport walking transport.
 * <p>
 * Implementations of WalkTransport transfer individual objects one at a a time
 * from the loose objects directory, or entire packs if the source side does not
 * have the object as a loose object.
 * <p>
 * WalkTransports are not as efficient as {@link PackTransport} instances, but
 * can be useful in situations where a pack transport is not acceptable.
 * 
 * @see WalkFetchConnection
 */
abstract class WalkTransport extends Transport {
	WalkTransport(final Repository local, final URIish u) {
		super(local, u);
	}
}
