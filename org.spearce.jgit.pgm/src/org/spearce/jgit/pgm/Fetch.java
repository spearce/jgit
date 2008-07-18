/*
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

package org.spearce.jgit.pgm;

import java.util.ArrayList;
import java.util.List;

import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.TextProgressMonitor;
import org.spearce.jgit.transport.FetchResult;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.TrackingRefUpdate;
import org.spearce.jgit.transport.Transport;

class Fetch extends TextBuiltin {
	@Override
	public void execute(String[] args) throws Exception {
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
		final FetchResult r;
		try {
			final List<RefSpec> toget = new ArrayList<RefSpec>();
			for (; argi < args.length; argi++)
				toget.add(new RefSpec(args[argi]));
			r = tn.fetch(new TextProgressMonitor(), toget);
			if (r.getTrackingRefUpdates().isEmpty())
				return;
		} finally {
			tn.close();
		}

		out.print("From ");
		out.print(tn.getURI());
		out.println();
		for (final TrackingRefUpdate u : r.getTrackingRefUpdates()) {
			final char type = shortTypeOf(u.getResult());
			final String longType = longTypeOf(u);
			final String src = abbreviateRef(u.getRemoteName(), false);
			final String dst = abbreviateRef(u.getLocalName(), true);

			out.format(" %c %-17s %-10s -> %s", type, longType, src, dst);
			out.println();
		}
	}

	private static String longTypeOf(final TrackingRefUpdate u) {
		final RefUpdate.Result r = u.getResult();
		if (r == RefUpdate.Result.LOCK_FAILURE)
			return "[lock fail]";

		if (r == RefUpdate.Result.IO_FAILURE)
			return "[i/o error]";

		if (r == RefUpdate.Result.NEW) {
			if (u.getRemoteName().startsWith(REFS_HEADS))
				return "[new branch]";
			else if (u.getLocalName().startsWith(REFS_TAGS))
				return "[new tag]";
			return "[new]";
		}

		if (r == RefUpdate.Result.FORCED) {
			final String aOld = abbreviateObject(u.getOldObjectId());
			final String aNew = abbreviateObject(u.getNewObjectId());
			return aOld + "..." + aNew;
		}

		if (r == RefUpdate.Result.FAST_FORWARD) {
			final String aOld = abbreviateObject(u.getOldObjectId());
			final String aNew = abbreviateObject(u.getNewObjectId());
			return aOld + ".." + aNew;
		}

		if (r == RefUpdate.Result.REJECTED)
			return "[rejected]";
		if (r == RefUpdate.Result.NO_CHANGE)
			return "[up to date]";
		return "[" + r.name() + "]";
	}

	private static char shortTypeOf(final RefUpdate.Result r) {
		if (r == RefUpdate.Result.LOCK_FAILURE)
			return '!';
		if (r == RefUpdate.Result.IO_FAILURE)
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
