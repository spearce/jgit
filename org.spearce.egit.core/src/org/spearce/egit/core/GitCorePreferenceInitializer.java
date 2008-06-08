/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/** Initializes plugin preferences with default values. */
public class GitCorePreferenceInitializer extends AbstractPreferenceInitializer {
	private static final int MB = 1024 * 1024;

	public void initializeDefaultPreferences() {
		final Preferences p = Activator.getDefault().getPluginPreferences();

		p.setDefault(GitCorePreferences.core_packedGitWindowSize, 8 * 1024);
		p.setDefault(GitCorePreferences.core_packedGitLimit, 10 * MB);
		p.setDefault(GitCorePreferences.core_packedGitMMAP, false);
		p.setDefault(GitCorePreferences.core_deltaBaseCacheLimit, 10 * MB);
	}
}
