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
