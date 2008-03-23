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

import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.revwalk.RevCommit;

class Log extends RevWalkTextBuiltin {
	@Override
	protected void show(final RevCommit c) throws Exception {
		out.print("commit ");
		c.getId().copyTo(outbuffer, out);
		out.println();

		final Commit parsed = c.asCommit(walk);
		out.print("Author: ");
		out.print(parsed.getAuthor().getName());
		out.print(" <");
		out.print(parsed.getAuthor().getEmailAddress());
		out.print(">");
		out.println();

		out.print("Date:   ");
		out.print(parsed.getAuthor().getWhen());
		out.println();

		out.println();
		final String[] lines = parsed.getMessage().split("\n");
		for (final String s : lines) {
			out.print("    ");
			out.print(s);
			out.println();
		}

		out.println();
	}
}
