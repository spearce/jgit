/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.push;

import org.spearce.egit.core.op.PushOperationResult;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.URIish;

/**
 * Data class representing row (element) of table with push results.
 * <p>
 * Each row is associated with one ref update, while each column is associated
 * with one URI (remote repository).
 *
 * @see PushOperationResult
 * @see RefUpdateContentProvider
 */
class RefUpdateElement {
	private final String srcRefName;

	private final String dstRefName;

	private final PushOperationResult result;

	RefUpdateElement(final PushOperationResult result, final String srcRef,
			final String dstRef) {
		this.result = result;
		this.srcRefName = srcRef;
		this.dstRefName = dstRef;
	}

	String getSrcRefName() {
		return srcRefName;
	}

	String getDstRefName() {
		return dstRefName;
	}

	boolean isDelete() {
		// Assuming that we never use ObjectId.zeroId() in GUI.
		// (no need to compare to it).
		return srcRefName == null;
	}

	boolean isSuccessfulConnection(final URIish uri) {
		return result.isSuccessfulConnection(uri);
	}

	String getErrorMessage(final URIish uri) {
		return result.getErrorMessage(uri);
	}

	RemoteRefUpdate getRemoteRefUpdate(final URIish uri) {
		return result.getPushResult(uri).getRemoteUpdate(dstRefName);
	}

	Ref getAdvertisedRemoteRef(final URIish uri) {
		return result.getPushResult(uri).getAdvertisedRef(dstRefName);
	}
}
