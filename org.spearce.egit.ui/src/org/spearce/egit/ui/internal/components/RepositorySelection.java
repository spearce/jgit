/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/

package org.spearce.egit.ui.internal.components;

import java.util.Collections;
import java.util.List;

import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.URIish;

/**
 * Data class representing selection of remote repository made by user.
 * Selection is an URI or remote repository configuration.
 * <p>
 * Each immutable instance has at least one of two class fields (URI, remote
 * config) set to null. null value indicates that it has illegal value or this
 * form of repository selection is not selected.
 * <p>
 * If remote configuration is selected, it always has non-empty URIs list.
 */
public class RepositorySelection {
	private URIish uri;

	private RemoteConfig config;

	static final RepositorySelection INVALID_SELECTION = new RepositorySelection(
			null, null);

	/**
	 * @param uri
	 *            the new specified URI. null if the new URI is invalid or user
	 *            chosen to specify repository as remote config instead of URI.
	 * @param config
	 *            the new remote config. null if user chosen to specify
	 *            repository as URI.
	 */
	RepositorySelection(final URIish uri, final RemoteConfig config) {
		if (config != null && uri != null)
			throw new IllegalArgumentException(
					"URI and config cannot be set at the same time.");
		this.config = config;
		this.uri = uri;
	}

	/**
	 * @return the selected URI (if specified by user as valid custom URI) or
	 *         first URI from selected configuration (if specified by user as
	 *         May be null if there is no valid selection.
	 */
	public URIish getURI() {
		if (isConfigSelected())
			return config.getURIs().get(0);
		return uri;
	}

	/**
	 * @return list of all selected URIs - either the one specified as custom
	 *         URI or all URIs from selected configuration. May be null in case
	 *         of no valid selection.
	 */
	public List<URIish> getAllURIs() {
		if (isURISelected())
			return Collections.singletonList(uri);
		if (isConfigSelected())
			return config.getURIs();
		return null;
	}

	/**
	 * @return the selected remote configuration. null if user chosen to select
	 *         repository as URI.
	 */
	public RemoteConfig getConfig() {
		return config;
	}

	/**
	 * @return selected remote configuration name or null if selection is not a
	 *         remote configuration.
	 */
	public String getConfigName() {
		if (isConfigSelected())
			return config.getName();
		return null;
	}

	/**
	 * @return true if selection contains valid URI or remote config, false if
	 *         there is no valid selection.
	 */
	public boolean isValidSelection() {
		return uri != null || config != null;
	}

	/**
	 * @return true if user selected valid URI, false if user selected invalid
	 *         URI or remote config.
	 */
	public boolean isURISelected() {
		return uri != null;
	}

	/**
	 * @return true if user selected remote configuration, false if user
	 *         selected (invalid or valid) URI.
	 */
	public boolean isConfigSelected() {
		return config != null;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof RepositorySelection) {
			final RepositorySelection other = (RepositorySelection) obj;
			if (uri == null ^ other.uri == null)
				return false;
			if (uri != null && !uri.equals(other.uri))
				return false;

			if (config != other.config)
				return false;

			return true;
		} else
			return false;
	}
}
