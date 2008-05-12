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

import java.io.PrintWriter;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;

class FetchHeadRecord {
	private static final String REFS_HEADS = Constants.HEADS_PREFIX + "/";

	private static final String REFS_REMOTES = Constants.REMOTES_PREFIX + "/";

	private static final String REFS_TAGS = Constants.TAGS_PREFIX + "/";

	ObjectId newValue;

	boolean notForMerge;

	String sourceName;

	URIish sourceURI;

	void write(final PrintWriter pw) {
		final String type;
		final String name;
		if (sourceName.startsWith(REFS_HEADS)) {
			type = "branch";
			name = sourceName.substring(REFS_HEADS.length());
		} else if (sourceName.startsWith(REFS_TAGS)) {
			type = "tag";
			name = sourceName.substring(REFS_TAGS.length());
		} else if (sourceName.startsWith(REFS_REMOTES)) {
			type = "remote branch";
			name = sourceName.substring(REFS_REMOTES.length());
		} else {
			type = "";
			name = sourceName;
		}

		pw.print(newValue);
		pw.print('\t');
		if (notForMerge)
			pw.print("not-for-merge");
		pw.print('\t');
		pw.print(type);
		pw.print(" '");
		pw.print(name);
		pw.print("' of ");
		pw.print(sourceURI);
		pw.println();
	}
}
