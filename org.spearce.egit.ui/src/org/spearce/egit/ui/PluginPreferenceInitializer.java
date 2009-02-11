/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
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

		prefs.setDefault(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS, true);
		prefs.setDefault(UIPreferences.DECORATOR_RECURSIVE_LIMIT,
				Integer.MAX_VALUE);
		prefs.setDefault(UIPreferences.DECORATOR_FILETEXT_DECORATION,
				UIText.DecoratorPreferencesPage_fileFormatDefault);
		prefs.setDefault(UIPreferences.DECORATOR_FOLDERTEXT_DECORATION,
				UIText.DecoratorPreferencesPage_folderFormatDefault);
		prefs.setDefault(UIPreferences.DECORATOR_PROJECTTEXT_DECORATION,
				UIText.DecoratorPreferencesPage_projectFormatDefault);
		prefs.setDefault(UIPreferences.DECORATOR_SHOW_TRACKED_ICON, true);
		prefs.setDefault(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON, true);
		prefs.setDefault(UIPreferences.DECORATOR_SHOW_STAGED_ICON, true);
		prefs.setDefault(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON, true);
		prefs.setDefault(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON, true);

		w = new int[] { 500, 500 };
		UIPreferences.setDefault(prefs,
				UIPreferences.RESOURCEHISTORY_GRAPH_SPLIT, w);
		w = new int[] { 700, 300 };
		UIPreferences.setDefault(prefs,
				UIPreferences.RESOURCEHISTORY_REV_SPLIT, w);

		prefs.setDefault(UIPreferences.FINDTOOLBAR_IGNORE_CASE, true);
		prefs.setDefault(UIPreferences.FINDTOOLBAR_FIND_IN, 2);
	}

}
