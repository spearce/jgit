/*
 *  Copyright (C) 2008  Roger C. Soares <rogersoares@intelinet.com.br>
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
package org.spearce.egit.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * Plugin extension point to initialize the plugin runtime preferences.
 */
public class PluginPreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 * Calls super constructor.
	 */
	public PluginPreferenceInitializer() {
		super();
	}

	/**
	 * This method initializes the plugin preferences with default values.
	 */
	public void initializeDefaultPreferences() {
		Preferences prefs = Activator.getDefault().getPluginPreferences();
		int[] w;

		prefs.setDefault(UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP, true);
		prefs.setDefault(UIPreferences.RESOURCEHISTORY_SHOW_REV_DETAIL, true);
		prefs.setDefault(UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT, true);
		prefs.setDefault(UIPreferences.RESOURCEHISTORY_SHOW_TOOLTIPS, false);

		w = new int[] { 500, 500 };
		UIPreferences.setDefault(prefs,
				UIPreferences.RESOURCEHISTORY_GRAPH_SPLIT, w);
		w = new int[] { 700, 300 };
		UIPreferences.setDefault(prefs,
				UIPreferences.RESOURCEHISTORY_REV_SPLIT, w);

		prefs.setDefault(UIPreferences.FINDTOOLBAR_IGNORE_CASE, true);
		prefs.setDefault(UIPreferences.FINDTOOLBAR_COMMIT_ID, true);
		prefs.setDefault(UIPreferences.FINDTOOLBAR_COMMENTS, true);
		prefs.setDefault(UIPreferences.FINDTOOLBAR_AUTHOR, false);
		prefs.setDefault(UIPreferences.FINDTOOLBAR_COMMITTER, false);
	}

}
