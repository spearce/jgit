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
	public static String GitCloneWizard_title;

	/** */
	public static String GitCloneWizard_jobName;

	/** */
	public static String GitCloneWizard_failed;

	/** */
	public static String CloneSourcePage_title;

	/** */
	public static String CloneSourcePage_description;

	/** */
	public static String CloneSourcePage_groupLocation;

	/** */
	public static String CloneSourcePage_groupAuthentication;

	/** */
	public static String CloneSourcePage_groupConnection;

	/** */
	public static String CloneSourcePage_promptURI;

	/** */
	public static String CloneSourcePage_promptHost;

	/** */
	public static String CloneSourcePage_promptPath;

	/** */
	public static String CloneSourcePage_promptUser;

	/** */
	public static String CloneSourcePage_promptPassword;

	/** */
	public static String CloneSourcePage_promptScheme;

	/** */
	public static String CloneSourcePage_promptPort;

	/** */
	public static String CloneSourcePage_fieldRequired;

	/** */
	public static String CloneSourcePage_fieldNotSupported;

	/** */
	public static String CloneSourcePage_fileNotFound;

	/** */
	public static String CloneSourcePage_internalError;

	/** */
	public static String SourceBranchPage_branchList;

	/** */
	public static String SourceBranchPage_selectAll;

	/** */
	public static String SourceBranchPage_selectNone;

	/** */
	public static String SourceBranchPage_errorBranchRequired;

	/** */
	public static String SourceBranchPage_transportError;

	/** */
	public static String SourceBranchPage_interrupted;

	/** */
	public static String SourceBranchPage_cannotListBranches;

	/** */
	public static String CloneDestinationPage_title;

	/** */
	public static String CloneDestinationPage_description;

	/** */
	public static String CloneDestinationPage_groupDestination;

	/** */
	public static String CloneDestinationPage_groupConfiguration;

	/** */
	public static String CloneDestinationPage_promptDirectory;

	/** */
	public static String CloneDestinationPage_promptInitialBranch;

	/** */
	public static String CloneDestinationPage_promptRemoteName;

	/** */
	public static String CloneDestinationPage_fieldRequired;

	/** */
	public static String CloneDestinationPage_browseButton;

	/** */
	public static String CloneDestinationPage_errorExists;

	/** */
	public static String Decorator_failedLazyLoading;

	/** */
	public static String QuickDiff_failedLoading;

	/** */
	public static String ResourceHistory_toggleCommentWrap;

	/** */
	public static String ResourceHistory_toggleCommentFill;
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
	public static String HistoryPage_findbar_findTooltip;

	/** */
	public static String HistoryPage_findbar_find;

	/** */
	public static String HistoryPage_findbar_next;

	/** */
	public static String HistoryPage_findbar_previous;

	/** */
	public static String HistoryPage_findbar_ignorecase;

	/** */
	public static String HistoryPage_findbar_commit;

	/** */
	public static String HistoryPage_findbar_comments;

	/** */
	public static String HistoryPage_findbar_author;

	/** */
	public static String HistoryPage_findbar_committer;

	/** */
	public static String HistoryPage_findbar_changeto_commit;

	/** */
	public static String HistoryPage_findbar_changeto_comments;

	/** */
	public static String HistoryPage_findbar_changeto_author;

	/** */
	public static String HistoryPage_findbar_changeto_committer;

	/** */
	public static String HistoryPage_findbar_exceeded;

	/** */
	public static String HistoryPage_findbar_notFound;

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
