/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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

import org.eclipse.osgi.util.NLS;

/**
 * Text resources for the plugin. Strings here can be i18n-ed simpler and avoid
 * duplicating strings.
 */
public class UIText extends NLS {
	/** */
	public static String SharingWizard_windowTitle;

	/** */
	public static String SharingWizard_failed;

	/** */
	public static String GenericOperationFailed;

	/** */
	public static String ExistingOrNewPage_title;

	/** */
	public static String ExistingOrNewPage_description;

	/** */
	public static String ExistingOrNewPage_groupHeader;

	/** */
	public static String ExistingOrNewPage_useExisting;

	/** */
	public static String ExistingOrNewPage_createNew;

	/** */
	public static String Decorator_failedLazyLoading;

	/** */
	public static String QuickDiff_failedLoading;

	/** */
	public static String ResourceHistory_toggleCommentWrap;
	/** */
	public static String ResourceHistory_toggleRevDetail;
	/** */
	public static String ResourceHistory_toggleRevComment;
	/** */
	public static String ResourceHistory_toggleTooltips;

	/** */
	public static String HistoryPage_authorColumn;
	/** */
	public static String HistoryPage_dateColumn;
	/** */
	public static String HistoryPage_pathnameColumn;
	/** */
	public static String HistoryPage_refreshJob;

	/** */
	public static String HistoryPreferencePage_title;

	/** */
	public static String WindowCachePreferencePage_title;
	/** */
	public static String WindowCachePreferencePage_packedGitWindowSize;
	/** */
	public static String WindowCachePreferencePage_packedGitLimit;
	/** */
	public static String WindowCachePreferencePage_deltaBaseCacheLimit;
	/** */
	public static String WindowCachePreferencePage_packedGitMMAP;
	/** */
	public static String WindowCachePreferencePage_note;
	/** */
	public static String WindowCachePreferencePage_needRestart;

	static {
		initializeMessages(UIText.class.getPackage().getName() + ".uitext",
				UIText.class);
	}
}
