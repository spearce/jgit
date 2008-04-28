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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.spearce.jgit.lib.Ref;

/**
 * Final status after a successful fetch from a remote repository.
 * 
 * @see Transport#fetch(org.spearce.jgit.lib.ProgressMonitor, Collection)
 */
public class FetchResult {
	private final SortedMap<String, TrackingRefUpdate> updates;

	private final List<FetchHeadRecord> forMerge;

	private Map<String, Ref> advertisedRefs;

	FetchResult() {
		updates = new TreeMap<String, TrackingRefUpdate>();
		forMerge = new ArrayList<FetchHeadRecord>();
		advertisedRefs = Collections.<String, Ref> emptyMap();
	}

	void add(final TrackingRefUpdate u) {
		updates.put(u.getLocalName(), u);
	}

	void add(final FetchHeadRecord r) {
		if (!r.notForMerge)
			forMerge.add(r);
	}

	void setAdvertisedRefs(final Map<String, Ref> ar) {
		advertisedRefs = ar;
	}

	/**
	 * Get the complete list of refs advertised by the remote.
	 * <p>
	 * The returned refs may appear in any order. If the caller needs these to
	 * be sorted, they should be copied into a new array or List and then sorted
	 * by the caller as necessary.
	 * 
	 * @return available/advertised refs. Never null. Not modifiable. The
	 *         collection can be empty if the remote side has no refs (it is an
	 *         empty/newly created repository).
	 */
	public Collection<Ref> getAdvertisedRefs() {
		return advertisedRefs.values();
	}

	/**
	 * Get a single advertised ref by name.
	 * <p>
	 * The name supplied should be valid ref name. To get a peeled value for a
	 * ref (aka <code>refs/tags/v1.0^{}</code>) use the base name (without
	 * the <code>^{}</code> suffix) and look at the peeled object id.
	 * 
	 * @param name
	 *            name of the ref to obtain.
	 * @return the requested ref; null if the remote did not advertise this ref.
	 */
	public final Ref getAdvertisedRef(final String name) {
		return advertisedRefs.get(name);
	}

	/**
	 * Get the status of all local tracking refs that were updated.
	 * 
	 * @return unmodifiable collection of local updates. Never null. Empty if
	 *         there were no local tracking refs updated.
	 */
	public Collection<TrackingRefUpdate> getTrackingRefUpdates() {
		return Collections.unmodifiableCollection(updates.values());
	}

	/**
	 * Get the status for a specific local tracking ref update.
	 * 
	 * @param localName
	 *            name of the local ref (e.g. "refs/remotes/origin/master").
	 * @return status of the local ref; null if this local ref was not touched
	 *         during this fetch.
	 */
	public TrackingRefUpdate getTrackingRefUpdate(final String localName) {
		return updates.get(localName);
	}
}
