/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.URIish;

/**
 * Interface for listeners of repository selection events from repository
 * selection dialogs.
 */
public interface RepositorySelectionListener {
	/**
	 * Notify the receiver that the repository selection has changed. Each time
	 * at least one argument of this call is null, which indicates that it has
	 * illegal value or this form of repository selection is not selected.
	 *
	 * @param newURI
	 *            the new specified URI. null if the new URI is invalid or user
	 *            chosen to specify repository as remote config instead of URI.
	 * @param newConfig
	 *            the new remote config. null if user chosen to specify
	 *            repository as URI.
	 */
	public void selectionChanged(URIish newURI, RemoteConfig newConfig);
}
