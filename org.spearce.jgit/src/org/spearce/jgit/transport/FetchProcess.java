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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.LockFile;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.revwalk.ObjectWalk;
import org.spearce.jgit.revwalk.RevWalk;

class FetchProcess {
	/** Transport we will fetch over. */
	private final Transport transport;

	/** List of things we want to fetch from the remote repository. */
	private final Collection<RefSpec> toFetch;

	/** Set of refs we will actually wind up asking to obtain. */
	private final HashMap<ObjectId, Ref> askFor = new HashMap<ObjectId, Ref>();

	/** Updates to local tracking branches (if any). */
	private final ArrayList<TrackingRefUpdate> localUpdates = new ArrayList<TrackingRefUpdate>();

	/** Records to be recorded into FETCH_HEAD. */
	private final ArrayList<FetchHeadRecord> fetchHeadUpdates = new ArrayList<FetchHeadRecord>();

	FetchProcess(final Transport t, final Collection<RefSpec> f) {
		transport = t;
		toFetch = f;
	}

	void execute(final ProgressMonitor monitor, final FetchResult result)
			throws NotSupportedException, TransportException {
		FetchConnection conn;

		askFor.clear();
		localUpdates.clear();
		fetchHeadUpdates.clear();

		conn = transport.openFetch();
		try {
			result.setAdvertisedRefs(conn.getCachedRefs());
			final Set<Ref> matched = new HashSet<Ref>();
			for (final RefSpec spec : toFetch) {
				if (spec.isWildcard())
					expandWildcard(conn, spec, matched);
				else
					expandSingle(conn, spec, matched);
			}
			if (!askFor.isEmpty() && !askForIsComplete())
				conn.fetch(monitor, askFor.values());
		} finally {
			conn.close();
			conn = null;
		}

		final RevWalk walk = new RevWalk(transport.local);
		for (TrackingRefUpdate u : localUpdates) {
			try {
				u.update(walk);
				result.add(u);
			} catch (IOException err) {
				throw new TransportException("Failure updating tracking ref "
						+ u.getLocalName() + ": " + err.getMessage(), err);
			}
		}

		if (!fetchHeadUpdates.isEmpty()) {
			try {
				updateFETCH_HEAD(result);
			} catch (IOException err) {
				throw new TransportException("Failure updating FETCH_HEAD: "
						+ err.getMessage(), err);
			}
		}
	}

	private void updateFETCH_HEAD(final FetchResult result) throws IOException {
		final LockFile lock = new LockFile(new File(transport.local
				.getDirectory(), "FETCH_HEAD"));
		if (lock.lock()) {
			final PrintWriter pw = new PrintWriter(new OutputStreamWriter(lock
					.getOutputStream())) {
				@Override
				public void println() {
					print('\n');
				}
			};
			for (final FetchHeadRecord h : fetchHeadUpdates) {
				h.write(pw);
				result.add(h);
			}
			pw.close();
			lock.commit();
		}
	}

	private boolean askForIsComplete() throws TransportException {
		try {
			final ObjectWalk ow = new ObjectWalk(transport.local);
			for (final ObjectId want : askFor.keySet())
				ow.markStart(ow.parseAny(want));
			for (final Ref ref : transport.local.getAllRefs().values())
				ow.markUninteresting(ow.parseAny(ref.getObjectId()));
			ow.checkConnectivity();
			return true;
		} catch (MissingObjectException e) {
			return false;
		} catch (IOException e) {
			throw new TransportException("Unable to check connectivity.", e);
		}
	}

	private void expandWildcard(final FetchConnection conn, final RefSpec spec,
			final Set<Ref> matched) throws TransportException {
		for (final Ref src : conn.getRefs()) {
			if (spec.matchSource(src) && matched.add(src))
				want(src, spec.expandFromSource(src));
		}
	}

	private void expandSingle(final FetchConnection conn, final RefSpec spec,
			final Set<Ref> matched) throws TransportException {
		final Ref src = conn.getRef(spec.getSource());
		if (src == null) {
			throw new TransportException("Remote does not have "
					+ spec.getSource() + " available for fetch.");
		}
		if (matched.add(src))
			want(src, spec);
	}

	private void want(final Ref src, final RefSpec spec)
			throws TransportException {
		final ObjectId newId = src.getObjectId();
		if (spec.getDestination() != null) {
			try {
				final TrackingRefUpdate tru = createUpdate(spec, newId);
				if (newId.equals(tru.getOldObjectId()))
					return;
				localUpdates.add(tru);
			} catch (IOException err) {
				// Bad symbolic ref? That is the most likely cause.
				//
				throw new TransportException("Cannot resolve"
						+ " local tracking ref " + spec.getDestination()
						+ " for updating.", err);
			}
		}

		askFor.put(newId, src);

		final FetchHeadRecord fhr = new FetchHeadRecord();
		fhr.newValue = newId;
		fhr.notForMerge = spec.getDestination() != null;
		fhr.sourceName = src.getName();
		fhr.sourceURI = transport.getURI();
		fetchHeadUpdates.add(fhr);
	}

	private TrackingRefUpdate createUpdate(final RefSpec spec,
			final ObjectId newId) throws IOException {
		return new TrackingRefUpdate(transport.local, spec, newId, "fetch");
	}
}
