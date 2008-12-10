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

import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.TextProgressMonitor;
import org.spearce.jgit.transport.FetchResult;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.TrackingRefUpdate;
import org.spearce.jgit.transport.Transport;

@Command(common = true, usage = "Update remote refs from another repository")
class Fetch extends TextBuiltin {
	@Option(name = "--verbose", aliases = { "-v" }, usage = "be more verbose")
	private boolean verbose;

	@Option(name = "--fsck", usage = "perform fsck style checks on receive")
	private Boolean fsck;

	@Option(name = "--no-fsck")
	void nofsck(final boolean ignored) {
		fsck = Boolean.FALSE;
	}

	@Option(name = "--thin", usage = "fetch thin pack")
	private Boolean thin;

	@Option(name = "--no-thin")
	void nothin(final boolean ignored) {
		thin = Boolean.FALSE;
	}

	@Argument(index = 0, metaVar = "uri-ish")
	private String remote = "origin";

	@Argument(index = 1, metaVar = "refspec")
	private List<RefSpec> toget;

	@Override
	protected void run() throws Exception {
		final Transport tn = Transport.open(db, remote);
		if (fsck != null)
			tn.setCheckFetchedObjects(fsck.booleanValue());
		if (thin != null)
			tn.setFetchThin(thin.booleanValue());
		final FetchResult r;
		try {
			r = tn.fetch(new TextProgressMonitor(), toget);
			if (r.getTrackingRefUpdates().isEmpty())
				return;
		} finally {
			tn.close();
		}

		boolean shownURI = false;
		for (final TrackingRefUpdate u : r.getTrackingRefUpdates()) {
			if (!verbose && u.getResult() == RefUpdate.Result.NO_CHANGE)
				continue;

			final char type = shortTypeOf(u.getResult());
			final String longType = longTypeOf(u);
			final String src = abbreviateRef(u.getRemoteName(), false);
			final String dst = abbreviateRef(u.getLocalName(), true);

			if (!shownURI) {
				out.print("From ");
				out.print(tn.getURI());
				out.println();
				shownURI = true;
			}

			out.format(" %c %-17s %-10s -> %s", type, longType, src, dst);
			out.println();
		}
	}

	private String longTypeOf(final TrackingRefUpdate u) {
		final RefUpdate.Result r = u.getResult();
		if (r == RefUpdate.Result.LOCK_FAILURE)
			return "[lock fail]";

		if (r == RefUpdate.Result.IO_FAILURE)
			return "[i/o error]";

		if (r == RefUpdate.Result.NEW) {
			if (u.getRemoteName().startsWith(Constants.R_HEADS))
				return "[new branch]";
			else if (u.getLocalName().startsWith(Constants.R_TAGS))
				return "[new tag]";
			return "[new]";
		}

		if (r == RefUpdate.Result.FORCED) {
			final String aOld = u.getOldObjectId().abbreviate(db).name();
			final String aNew = u.getNewObjectId().abbreviate(db).name();
			return aOld + "..." + aNew;
		}

		if (r == RefUpdate.Result.FAST_FORWARD) {
			final String aOld = u.getOldObjectId().abbreviate(db).name();
			final String aNew = u.getNewObjectId().abbreviate(db).name();
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
