/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.URIish;

/**
 * Data class storing push operation update specifications for each remote
 * repository.
 * <p>
 * One instance is dedicated for one push operation: either push to one URI or
 * to many URIs.
 *
 * @see PushOperation
 */
public class PushOperationSpecification {
	private LinkedHashMap<URIish, Collection<RemoteRefUpdate>> urisRefUpdates;

	/**
	 * Create empty instance of specification.
	 * <p>
	 * URIs and ref updates should be configured
	 * {@link #addURIRefUpdates(URIish, Collection)} method.
	 */
	public PushOperationSpecification() {
		this.urisRefUpdates = new LinkedHashMap<URIish, Collection<RemoteRefUpdate>>();
	}

	/**
	 * Add remote repository URI with ref updates specification.
	 * <p>
	 * Ref updates are not in constructor - pay attention to not share them
	 * between different URIs ref updates or push operations.
	 * <p>
	 * Note that refUpdates can differ between URIs <b>only</b> by expected old
	 * object id field: {@link RemoteRefUpdate#getExpectedOldObjectId()}.
	 *
	 * @param uri
	 *            remote repository URI.
	 * @param refUpdates
	 *            collection of remote ref updates specifications.
	 */
	public void addURIRefUpdates(final URIish uri,
			Collection<RemoteRefUpdate> refUpdates) {
		urisRefUpdates.put(uri, refUpdates);
	}

	/**
	 * @return set of remote repositories URIish. Set is ordered in addition
	 *         sequence.
	 */
	public Set<URIish> getURIs() {
		return Collections.unmodifiableSet(urisRefUpdates.keySet());
	}

	/**
	 * @return number of remote repositories URI for this push operation.
	 */
	public int getURIsNumber() {
		return urisRefUpdates.keySet().size();
	}

	/**
	 * @param uri
	 *            remote repository URI.
	 * @return remote ref updates as specified by user for this URI.
	 */
	public Collection<RemoteRefUpdate> getRefUpdates(final URIish uri) {
		return Collections.unmodifiableCollection(urisRefUpdates.get(uri));
	}
}
