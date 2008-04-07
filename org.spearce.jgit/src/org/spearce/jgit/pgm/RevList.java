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
package org.spearce.jgit.pgm;

import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevFlag;

class RevList extends RevWalkTextBuiltin {
	@Override
	protected void show(final RevCommit c) throws Exception {
		if (c.has(RevFlag.UNINTERESTING))
			out.print('-');
		c.getId().copyTo(outbuffer, out);
		if (parents)
			for (int i = 0; i < c.getParentCount(); i++) {
				out.print(' ');
				c.getParent(i).getId().copyTo(outbuffer, out);
			}
		out.println();
	}
}
