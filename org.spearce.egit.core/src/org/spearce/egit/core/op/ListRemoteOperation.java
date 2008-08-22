/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.spearce.egit.core.CoreText;
import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.transport.Connection;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.URIish;

/**
 * Operation of listing remote repository advertised refs.
 */
public class ListRemoteOperation implements IRunnableWithProgress {
	private final Repository localDb;

	private final URIish uri;

	private Map<String, Ref> remoteRefsMap;

	/**
	 * Create listing operation for specified local repository (needed by
	 * transport) and remote repository URI.
	 *
	 * @param localDb
	 *            local repository (needed for transport) where fetch would
	 *            occur.
	 * @param uri
	 *            URI of remote repository to list.
	 */
	public ListRemoteOperation(final Repository localDb, final URIish uri) {
		this.localDb = localDb;
		this.uri = uri;
	}

	/**
	 * @return collection of refs advertised by remote side.
	 * @throws IllegalStateException
	 *             if error occurred during earlier remote refs listing.
	 */
	public Collection<Ref> getRemoteRefs() {
		checkState();
		return remoteRefsMap.values();
	}

	/**
	 * @param refName
	 *            remote ref name to search for.
	 * @return ref with specified refName or null if not found.
	 * @throws IllegalStateException
	 *             if error occurred during earlier remote refs listing.
	 */
	public Ref getRemoteRef(final String refName) {
		checkState();
		return remoteRefsMap.get(refName);
	}

	public void run(IProgressMonitor pm) throws InvocationTargetException,
			InterruptedException {
		Transport transport = null;
		Connection connection = null;
		try {
			transport = Transport.open(localDb, uri);

			if (pm != null)
				pm.beginTask(CoreText.ListRemoteOperation_title,
						IProgressMonitor.UNKNOWN);
			connection = transport.openFetch();
			remoteRefsMap = connection.getRefsMap();
		} catch (NotSupportedException e) {
			throw new InvocationTargetException(e);
		} catch (TransportException e) {
			throw new InvocationTargetException(e);
		} finally {
			if (connection != null)
				connection.close();
			if (transport != null)
				transport.close();
			if (pm != null)
				pm.done();
		}
	}

	private void checkState() {
		if (remoteRefsMap == null)
			throw new IllegalStateException(
					"Error occurred during remote repo listing, no refs available");
	}
}