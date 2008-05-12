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

import java.util.ArrayList;
import java.util.List;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.TextProgressMonitor;
import org.spearce.jgit.transport.FetchResult;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.TrackingRefUpdate;
import org.spearce.jgit.transport.Transport;

class Fetch extends TextBuiltin {
	private static final String REFS_HEADS = Constants.HEADS_PREFIX + "/";

	private static final String REFS_REMOTES = Constants.REMOTES_PREFIX + "/";

	private static final String REFS_TAGS = Constants.TAGS_PREFIX + "/";

	@Override
	void execute(String[] args) throws Exception {
		int argi = 0;
		for (; argi < args.length; argi++) {
			final String a = args[argi];
			if ("--".equals(a)) {
				argi++;
				break;
			} else
				break;
		}
		if (args.length == 0)
			args = new String[] { "origin" };

		final Transport tn = Transport.open(db, args[argi++]);
		final List<RefSpec> toget = new ArrayList<RefSpec>();
		for (; argi < args.length; argi++)
			toget.add(new RefSpec(args[argi]));
		final FetchResult r = tn.fetch(new TextProgressMonitor(), toget);
		if (r.getTrackingRefUpdates().isEmpty())
			return;

		out.print("From ");
		out.print(tn.getURI());
		out.println();
		for (final TrackingRefUpdate u : r.getTrackingRefUpdates()) {
			final char type = shortTypeOf(u.getResult());
			final String longType = longTypeOf(u);

			String src = u.getRemoteName();
			if (src.startsWith(REFS_HEADS))
				src = src.substring(REFS_HEADS.length());
			else if (src.startsWith(REFS_TAGS))
				src = src.substring(REFS_TAGS.length());

			String dst = u.getLocalName();
			if (dst.startsWith(REFS_HEADS))
				dst = dst.substring(REFS_HEADS.length());
			else if (dst.startsWith(REFS_TAGS))
				dst = dst.substring(REFS_TAGS.length());
			else if (dst.startsWith(REFS_REMOTES))
				dst = dst.substring(REFS_REMOTES.length());

			out.format(" %c %-17s %-10s -> %s", type, longType, src, dst);
			out.println();
		}
	}

	private static String longTypeOf(final TrackingRefUpdate u) {
		final RefUpdate.Result r = u.getResult();
		if (r == RefUpdate.Result.LOCK_FAILURE)
			return "[lock fail]";

		if (r == RefUpdate.Result.NEW) {
			if (u.getRemoteName().startsWith(REFS_HEADS))
				return "[new branch]";
			else if (u.getLocalName().startsWith(REFS_TAGS))
				return "[new tag]";
			return "[new]";
		}

		if (r == RefUpdate.Result.FORCED) {
			final String aOld = abbreviate(u.getOldObjectId());
			final String aNew = abbreviate(u.getNewObjectId());
			return aOld + "..." + aNew;
		}

		if (r == RefUpdate.Result.FAST_FORWARD) {
			final String aOld = abbreviate(u.getOldObjectId());
			final String aNew = abbreviate(u.getNewObjectId());
			return aOld + ".." + aNew;
		}

		if (r == RefUpdate.Result.REJECTED)
			return "[rejected]";
		if (r == RefUpdate.Result.NO_CHANGE)
			return "[up to date]";
		return "[" + r.name() + "]";
	}

	private static String abbreviate(final ObjectId id) {
		return id.toString().substring(0, 7);
	}

	private static char shortTypeOf(final RefUpdate.Result r) {
		if (r == RefUpdate.Result.LOCK_FAILURE)
			return '!';
		if (r == RefUpdate.Result.NEW)
			return '*';
		if (r == RefUpdate.Result.FORCED)
			return '+';
		if (r == RefUpdate.Result.FAST_FORWARD)
			return ' ';
		if (r == RefUpdate.Result.REJECTED)
			return '!';
		if (r == RefUpdate.Result.NO_CHANGE)
			return '=';
		return ' ';
	}
}
