/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
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
