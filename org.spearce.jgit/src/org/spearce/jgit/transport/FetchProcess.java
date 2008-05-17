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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Constants;
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

	/** How to handle annotated tags, if any are advertised. */
	private final TagOpt tagopt;

	/** Set of refs we will actually wind up asking to obtain. */
	private final HashMap<ObjectId, Ref> askFor = new HashMap<ObjectId, Ref>();

	/** Updates to local tracking branches (if any). */
	private final ArrayList<TrackingRefUpdate> localUpdates = new ArrayList<TrackingRefUpdate>();

	/** Records to be recorded into FETCH_HEAD. */
	private final ArrayList<FetchHeadRecord> fetchHeadUpdates = new ArrayList<FetchHeadRecord>();

	private FetchConnection conn;

	FetchProcess(final Transport t, final Collection<RefSpec> f, final TagOpt o) {
		transport = t;
		toFetch = f;
		tagopt = o;
	}

	void execute(final ProgressMonitor monitor, final FetchResult result)
			throws NotSupportedException, TransportException {
		askFor.clear();
		localUpdates.clear();
		fetchHeadUpdates.clear();

		conn = transport.openFetch();
		try {
			result.setAdvertisedRefs(conn.getCachedRefs());
			final Set<Ref> matched = new HashSet<Ref>();
			for (final RefSpec spec : toFetch) {
				if (spec.isWildcard())
					expandWildcard(spec, matched);
				else
					expandSingle(spec, matched);
			}

			Collection<Ref> additionalTags = Collections.<Ref> emptyList();
			if (tagopt == TagOpt.AUTO_FOLLOW)
				additionalTags = expandAutoFollowTags();
			else if (tagopt == TagOpt.FETCH_TAGS)
				expandFetchTags();

			final boolean includedTags;
			if (!askFor.isEmpty() && !askForIsComplete()) {
				conn.fetch(monitor, askFor.values());
				includedTags = conn.didFetchIncludeTags();

				// Connection was used for object transfer. If we
				// do another fetch we must open a new connection.
				//
				closeConnection();
			} else {
				includedTags = false;
			}

			if (tagopt == TagOpt.AUTO_FOLLOW && !additionalTags.isEmpty()) {
				// There are more tags that we want to follow, but
				// not all were asked for on the initial request.
				//
				askFor.clear();
				for (final Ref r : additionalTags) {
					final ObjectId id = r.getPeeledObjectId();
					if (id == null || transport.local.hasObject(id))
						wantTag(r);
				}

				if (!askFor.isEmpty() && (!includedTags || !askForIsComplete())) {
					reopenConnection();
					if (!askFor.isEmpty())
						conn.doFetch(monitor, askFor.values());
				}
			}
		} finally {
			closeConnection();
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

	private void closeConnection() {
		if (conn != null) {
			conn.close();
			conn = null;
		}
	}

	private void reopenConnection() throws NotSupportedException,
			TransportException {
		if (conn != null)
			return;

		conn = transport.openFetch();

		// Since we opened a new connection we cannot be certain
		// that the system we connected to has the same exact set
		// of objects available (think round-robin DNS and mirrors
		// that aren't updated at the same time).
		//
		// We rebuild our askFor list using only the refs that the
		// new connection has offered to us.
		//
		final HashMap<ObjectId, Ref> avail = new HashMap<ObjectId, Ref>();
		for (final Ref r : conn.getRefs())
			avail.put(r.getObjectId(), r);

		final Collection<Ref> wants = new ArrayList<Ref>(askFor.values());
		askFor.clear();
		for (final Ref want : wants) {
			final Ref newRef = avail.get(want.getObjectId());
			if (newRef != null) {
				askFor.put(newRef.getObjectId(), newRef);
			} else {
				removeFetchHeadRecord(want.getObjectId());
				removeTrackingRefUpdate(want.getObjectId());
			}
		}
	}

	private void removeTrackingRefUpdate(final ObjectId want) {
		final Iterator<TrackingRefUpdate> i = localUpdates.iterator();
		while (i.hasNext()) {
			final TrackingRefUpdate u = i.next();
			if (u.getNewObjectId().equals(want))
				i.remove();
		}
	}

	private void removeFetchHeadRecord(final ObjectId want) {
		final Iterator<FetchHeadRecord> i = fetchHeadUpdates.iterator();
		while (i.hasNext()) {
			final FetchHeadRecord fh = i.next();
			if (fh.newValue.equals(want))
				i.remove();
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

	private void expandWildcard(final RefSpec spec, final Set<Ref> matched)
			throws TransportException {
		for (final Ref src : conn.getRefs()) {
			if (spec.matchSource(src) && matched.add(src))
				want(src, spec.expandFromSource(src));
		}
	}

	private void expandSingle(final RefSpec spec, final Set<Ref> matched)
			throws TransportException {
		final Ref src = conn.getRef(spec.getSource());
		if (src == null) {
			throw new TransportException("Remote does not have "
					+ spec.getSource() + " available for fetch.");
		}
		if (matched.add(src))
			want(src, spec);
	}

	private Collection<Ref> expandAutoFollowTags() throws TransportException {
		final Collection<Ref> additionalTags = new ArrayList<Ref>();
		final Map<String, Ref> have = transport.local.getAllRefs();
		for (final Ref r : conn.getRefs()) {
			if (!isTag(r))
				continue;
			if (r.getPeeledObjectId() == null) {
				additionalTags.add(r);
				continue;
			}

			final Ref local = have.get(r.getName());
			if (local != null) {
				if (!r.getObjectId().equals(local.getObjectId()))
					wantTag(r);
			} else if (askFor.containsKey(r.getPeeledObjectId())
					|| transport.local.hasObject(r.getPeeledObjectId()))
				wantTag(r);
			else
				additionalTags.add(r);
		}
		return additionalTags;
	}

	private void expandFetchTags() throws TransportException {
		final Map<String, Ref> have = transport.local.getAllRefs();
		for (final Ref r : conn.getRefs()) {
			if (!isTag(r))
				continue;
			final Ref local = have.get(r.getName());
			if (local == null || !r.getObjectId().equals(local.getObjectId()))
				wantTag(r);
		}
	}

	private void wantTag(final Ref r) throws TransportException {
		want(r, new RefSpec().setSource(r.getName())
				.setDestination(r.getName()));
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

	private static boolean isTag(final Ref r) {
		return isTag(r.getName());
	}

	private static boolean isTag(final String name) {
		return name.startsWith(Constants.TAGS_PREFIX + "/");
	}
}
