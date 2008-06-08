/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.clone;

import org.spearce.jgit.transport.URIish;

interface URIishChangeListener {
	/**
	 * Notify the receiver that the URI has changed.
	 * 
	 * @param newURI
	 *            the new URI. Null if the new URI is invalid.
	 */
	void uriishChanged(URIish newURI);
}
