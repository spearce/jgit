/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.transport.PushResult;
import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.URIish;

/**
 * Data class for storing push operation results for each remote repository/URI
 * being part of push operation.
 * <p>
 * One instance of this class is dedicated for result of one push operation:
 * either to one URI or to many URIs.
 *
 * @see PushOperation
 */
public class PushOperationResult {
	private LinkedHashMap<URIish, Entry> urisEntries;

	/**
	 * Construct empty push operation result.
	 */
	PushOperationResult() {
		this.urisEntries = new LinkedHashMap<URIish, Entry>();
	}

	/**
	 * Add push result for the repository (URI) with successful connection.
	 *
	 * @param uri
	 *            remote repository URI.
	 * @param result
	 *            push result.
	 */
	public void addOperationResult(final URIish uri, final PushResult result) {
		urisEntries.put(uri, new Entry(result));
	}

	/**
	 * Add error message for the repository (URI) with unsuccessful connection.
	 *
	 * @param uri
	 *            remote repository URI.
	 * @param errorMessage
	 *            failure error message.
	 */
	public void addOperationResult(final URIish uri, final String errorMessage) {
		urisEntries.put(uri, new Entry(errorMessage));
	}

	/**
	 * @return set of remote repositories URIish. Set is ordered in addition
	 *         sequence, which is usually the same as that from
	 *         {@link PushOperationSpecification}.
	 */
	public Set<URIish> getURIs() {
		return Collections.unmodifiableSet(urisEntries.keySet());
	}

	/**
	 * @param uri
	 *            remote repository URI.
	 * @return true if connection was successful for this repository (URI),
	 *         false if this operation ended with unsuccessful connection.
	 */
	public boolean isSuccessfulConnection(final URIish uri) {
		return urisEntries.get(uri).isSuccessfulConnection();
	}

	/**
	 * @return true if connection was successful for any repository (URI), false
	 *         otherwise.
	 */
	public boolean isSuccessfulConnectionForAnyURI() {
		for (final URIish uri : getURIs()) {
			if (isSuccessfulConnection(uri))
				return true;
		}
		return false;
	}

	/**
	 * @param uri
	 *            remote repository URI.
	 * @return push result for this repository (URI) or null if operation ended
	 *         with unsuccessful connection for this URI.
	 */
	public PushResult getPushResult(final URIish uri) {
		return urisEntries.get(uri).getResult();
	}

	/**
	 * @param uri
	 *            remote repository URI.
	 * @return error message for this repository (URI) or null if operation
	 *         ended with successful connection for this URI.
	 */
	public String getErrorMessage(final URIish uri) {
		return urisEntries.get(uri).getErrorMessage();
	}

	/**
	 * @return string being list of failed URIs with their error messages.
	 */
	public String getErrorStringForAllURis() {
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final URIish uri : getURIs()) {
			if (first)
				first = false;
			else
				sb.append(", ");
			sb.append(uri);
			sb.append(" (");
			sb.append(getErrorMessage(uri));
			sb.append(")");
		}
		return sb.toString();
	}

	/**
	 * Derive push operation specification from this push operation result.
	 * <p>
	 * Specification is created basing on URIs of remote repositories in this
	 * result that completed without connection errors, and remote ref updates
	 * from push results.
	 * <p>
	 * This method is targeted to provide support for 2-stage push, where first
	 * operation is dry run for user confirmation and second one is a real
	 * operation.
	 *
	 * @param requireUnchanged
	 *            if true, newly created copies of remote ref updates have
	 *            expected old object id set to previously advertised ref value
	 *            (remote ref won't be updated if it change in the mean time),
	 *            if false, newly create copies of remote ref updates have
	 *            expected object id set up as in this result source
	 *            specification.
	 * @return derived specification for another push operation.
	 * @throws IOException
	 *             when some previously locally available source ref is not
	 *             available anymore, or some error occurred during creation
	 *             locally tracking ref update.
	 *
	 */
	public PushOperationSpecification deriveSpecification(
			final boolean requireUnchanged) throws IOException {
		final PushOperationSpecification spec = new PushOperationSpecification();
		for (final URIish uri : getURIs()) {
			final PushResult pr = getPushResult(uri);
			if (pr == null)
				continue;

			final Collection<RemoteRefUpdate> oldUpdates = pr
					.getRemoteUpdates();
			final ArrayList<RemoteRefUpdate> newUpdates = new ArrayList<RemoteRefUpdate>(
					oldUpdates.size());
			for (final RemoteRefUpdate rru : oldUpdates) {
				final ObjectId expectedOldObjectId;
				if (requireUnchanged) {
					final Ref advertisedRef = getPushResult(uri)
							.getAdvertisedRef(rru.getRemoteName());
					if (advertisedRef == null)
						expectedOldObjectId = ObjectId.zeroId();
					else
						expectedOldObjectId = advertisedRef.getObjectId();
				} else
					expectedOldObjectId = rru.getExpectedOldObjectId();
				final RemoteRefUpdate newRru = new RemoteRefUpdate(rru,
						expectedOldObjectId);
				newUpdates.add(newRru);
			}
			spec.addURIRefUpdates(uri, newUpdates);
		}
		return spec;
	}

	/**
	 * This implementation returns true if all following conditions are met:
	 * <ul>
	 * <li>both objects result have the same set successfully connected
	 * repositories (URIs) - unsuccessful connections are discarded, AND <li>
	 * remote ref updates must match for each successful connection in sense of
	 * equal remoteName, equal status and equal newObjectId value.</li>
	 * </ul>
	 *
	 * @see Object#equals(Object)
	 * @param obj
	 *            other push operation result to compare to.
	 * @return true if object is equal to this one in terms of conditions
	 *         described above, false otherwise.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof PushOperationResult))
			return false;

		final PushOperationResult other = (PushOperationResult) obj;

		// Check successful connections/URIs two-ways:
		final Set<URIish> otherURIs = other.getURIs();
		for (final URIish uri : getURIs()) {
			if (isSuccessfulConnection(uri)
					&& (!otherURIs.contains(uri) || !other
							.isSuccessfulConnection(uri)))
				return false;
		}
		for (final URIish uri : other.getURIs()) {
			if (other.isSuccessfulConnection(uri)
					&& (!urisEntries.containsKey(uri) || !isSuccessfulConnection(uri)))
				return false;
		}

		for (final URIish uri : getURIs()) {
			if (!isSuccessfulConnection(uri))
				continue;

			final PushResult otherPushResult = other.getPushResult(uri);
			for (final RemoteRefUpdate rru : getPushResult(uri)
					.getRemoteUpdates()) {
				final RemoteRefUpdate otherRru = otherPushResult
						.getRemoteUpdate(rru.getRemoteName());
				if (otherRru == null)
					return false;
				if (otherRru.getStatus() != rru.getStatus()
						|| otherRru.getNewObjectId() != rru.getNewObjectId())
					return false;
			}
		}
		return true;
	}

	private static class Entry {
		private String errorMessage;

		private PushResult result;

		Entry(final PushResult result) {
			this.result = result;
		}

		Entry(final String errorMessage) {
			this.errorMessage = errorMessage;
		}

		boolean isSuccessfulConnection() {
			return result != null;
		}

		String getErrorMessage() {
			return errorMessage;
		}

		PushResult getResult() {
			return result;
		}
	}
}
