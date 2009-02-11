/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui;

import org.eclipse.core.runtime.Preferences;

/**
 * Preferences used by the plugin.
 * All plugin preferences shall be referenced by a constant in this class.
 */
public class UIPreferences {
	/** */
	public final static String RESOURCEHISTORY_SHOW_COMMENT_WRAP = "resourcehistory_show_comment_wrap";
	/** */
	public static final String RESOURCEHISTORY_SHOW_COMMENT_FILL = "resourcehistory_fill_comment_paragraph";
	/** */
	public final static String RESOURCEHISTORY_SHOW_REV_DETAIL = "resourcehistory_show_rev_detail";
	/** */
	public final static String RESOURCEHISTORY_SHOW_REV_COMMENT = "resourcehistory_show_rev_comment";
	/** */
	public final static String RESOURCEHISTORY_GRAPH_SPLIT = "resourcehistory_graph_split";
	/** */
	public final static String RESOURCEHISTORY_REV_SPLIT = "resourcehistory_rev_split";
	/** */
	public final static String RESOURCEHISTORY_SHOW_TOOLTIPS = "resourcehistory_show_tooltips";
	/** */
	public final static String RESOURCEHISTORY_SHOW_FINDTOOLBAR = "resourcehistory_show_findtoolbar";
	/** */
	public final static String FINDTOOLBAR_IGNORE_CASE = "findtoolbar_ignore_case";
	/** */
	public final static String FINDTOOLBAR_COMMIT_ID = "findtoolbar_commit_id";
	/** */
	public final static String FINDTOOLBAR_COMMENTS = "findtoolbar_comments";
	/** */
	public final static String FINDTOOLBAR_AUTHOR = "findtoolbar_author";
	/** */
	public final static String FINDTOOLBAR_COMMITTER = "findtoolbar_committer";
	/** */
	public final static String FINDTOOLBAR_FIND_IN = "findtoolbar_find_in";

	/** */
	public final static String THEME_CommitGraphNormalFont = "org.spearce.egit.ui.CommitGraphNormalFont";
	/** */
	public final static String THEME_CommitGraphHighlightFont = "org.spearce.egit.ui.CommitGraphHighlightFont";
	/** */
	public final static String THEME_CommitMessageFont = "org.spearce.egit.ui.CommitMessageFont";

	/** */
	public final static String DECORATOR_RECOMPUTE_ANCESTORS = "decorator_recompute_ancestors";
	/** */
	public final static String DECORATOR_RECURSIVE_LIMIT = "decorator_recursive_limit";
	/** */
	public final static String DECORATOR_FILETEXT_DECORATION = "decorator_filetext_decoration";
	/** */
	public final static String DECORATOR_FOLDERTEXT_DECORATION = "decorator_foldertext_decoration";
	/** */
	public final static String DECORATOR_PROJECTTEXT_DECORATION = "decorator_projecttext_decoration";
	/** */
	public final static String DECORATOR_SHOW_TRACKED_ICON = "decorator_show_tracked_icon";
	/** */
	public final static String DECORATOR_SHOW_UNTRACKED_ICON = "decorator_show_untracked_icon";
	/** */
	public final static String DECORATOR_SHOW_STAGED_ICON = "decorator_show_staged_icon";
	/** */
	public final static String DECORATOR_SHOW_CONFLICTS_ICON = "decorator_show_conflicts_icon";
	/** */
	public final static String DECORATOR_SHOW_ASSUME_VALID_ICON = "decorator_show_assume_valid_icon";

	/**
	 * Get the preference values associated with a fixed integer array.
	 * 
	 * @param prefs
	 *            the store to read.
	 * @param key
	 *            key name.
	 * @param cnt
	 *            number of entries in the returned array.
	 * @return the preference values for the array.
	 */
	public static int[] getIntArray(final Preferences prefs, final String key,
			final int cnt) {
		final String s = prefs.getString(key);
		final int[] r = new int[cnt];
		if (s != null) {
			final String[] e = s.split(",");
			for (int i = 0; i < Math.min(e.length, r.length); i++)
				r[i] = Integer.parseInt(e[i].trim());
		}
		return r;
	}

	/**
	 * Set the preference values associated with a fixed integer array.
	 * 
	 * @param prefs
	 *            the store to read.
	 * @param key
	 *            key name.
	 * @param data
	 *            entries to store.
	 */
	public static void setValue(final Preferences prefs, final String key,
			final int[] data) {
		final StringBuilder s = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			if (i > 0)
				s.append(',');
			s.append(data[i]);
		}
		prefs.setValue(key, s.toString());
	}

	/**
	 * Set the preference values associated with a fixed integer array.
	 * 
	 * @param prefs
	 *            the store to read.
	 * @param key
	 *            key nam
	 * @param data
	 *            entries to store.
	 */
	public static void setDefault(final Preferences prefs, final String key,
			final int[] data) {
		final StringBuilder s = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			if (i > 0)
				s.append(',');
			s.append(data[i]);
		}
		prefs.setDefault(key, s.toString());
	}
}
