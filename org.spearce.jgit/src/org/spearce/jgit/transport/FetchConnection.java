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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.Ref;

/**
 * Lists known refs from the remote and copies objects of selected refs.
 * <p>
 * A fetch connection typically connects to the <code>git-upload-pack</code>
 * service running where the remote repository is stored. This provides a
 * one-way object transfer service to copy objects from the remote repository
 * into this local repository.
 * <p>
 * Instances of a FetchConnection must be created by a {@link Transport} that
 * implements a specific object transfer protocol that both sides of the
 * connection understand.
 * <p>
 * FetchConnection instances are not thread safe and may be accessed by only one
 * thread at a time.
 * 
 * @see Transport
 */
public abstract class FetchConnection {
	private Map<String, Ref> cachedRefs = Collections.<String, Ref> emptyMap();

	/** Have we started {@link #fetch(ProgressMonitor, Collection)} yet? */
	private boolean startedFetch;

	Map<String, Ref> getCachedRefs() {
		return cachedRefs;
	}

	/**
	 * Denote the list of refs available on the remote repository.
	 * <p>
	 * Implementors should invoke this method once they have obtained the refs
	 * that are available from the remote repository.s
	 * 
	 * @param all
	 *            the complete list of refs the remote has to offer. This map
	 *            will be wrapped in an unmodifiable way to protect it, but it
	 *            does not get copied.
	 */
	protected void available(final Map<String, Ref> all) {
		cachedRefs = Collections.unmodifiableMap(all);
	}

	/**
	 * Get the complete list of refs advertised as available for fetching.
	 * <p>
	 * The returned refs may appear in any order. If the caller needs these to
	 * be sorted, they should be copied into a new array or List and then sorted
	 * by the caller as necessary.
	 * 
	 * @return available/advertised refs. Never null. Not modifiable. The
	 *         collection can be empty if the remote side has no refs (it is an
	 *         empty/newly created repository).
	 */
	public final Collection<Ref> getRefs() {
		return cachedRefs.values();
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
	public final Ref getRef(final String name) {
		return cachedRefs.get(name);
	}

	/**
	 * Fetch objects we don't have but that are reachable from advertised refs.
	 * 
	 * @param monitor
	 *            progress monitor to update the end-user about the amount of
	 *            work completed, or to indicate cancellation.
	 * @param want
	 *            one or more refs advertised by this connection that the caller
	 *            wants to store locally.
	 * @throws TransportException
	 *             objects could not be copied due to a network failure,
	 *             protocol error, or error on remote side.
	 */
	public final void fetch(final ProgressMonitor monitor,
			final Collection<Ref> want) throws TransportException {
		if (startedFetch)
			throw new TransportException("Only one fetch call supported.");
		startedFetch = true;
		doFetch(monitor, want);
	}

	/**
	 * Fetch objects this repository does not yet contain.
	 * <p>
	 * Implementations are free to use network connections as necessary to
	 * efficiently (for both client and server) transfer objects from the remote
	 * repository into this repository. When possible implementations should
	 * avoid replacing/overwriting/duplicating an object already available in
	 * the local destination repository. Locally available objects and packs
	 * should always be preferred over remotely available objects and packs.
	 * 
	 * @param monitor
	 *            progress feedback to inform the end-user about the status of
	 *            the object transfer. Implementors should poll the monitor at
	 *            regular intervals to look for cancellation requests from the
	 *            user.
	 * @param want
	 *            one or more refs that were previously passed to
	 *            {@link #available(Map)} by the implementation. These refs
	 *            indicate the objects the caller wants copied.
	 * @throws TransportException
	 *             objects could not be copied due to a network failure,
	 *             protocol error, or error on remote side.
	 */
	protected abstract void doFetch(ProgressMonitor monitor,
			Collection<Ref> want) throws TransportException;

	/**
	 * Close any resources used by this connection.
	 * <p>
	 * If the remote repository is contacted by a network socket this method
	 * must close that network socket, disconnecting the two peers. If the
	 * remote repository is actually local (same system) this method must close
	 * any open file handles used to read the "remote" repository.
	 */
	public abstract void close();
}
