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
	public static String ExistingOrNewPage_createInParent;

	/** */
	public static String GitCloneWizard_title;

	/** */
	public static String GitCloneWizard_jobName;

	/** */
	public static String GitCloneWizard_failed;

	/** */
	public static String RepositorySelectionPage_sourceSelectionTitle;

	/** */
	public static String RepositorySelectionPage_sourceSelectionDescription;

	/** */
	public static String RepositorySelectionPage_destinationSelectionTitle;

	/** */
	public static String RepositorySelectionPage_destinationSelectionDescription;

	/** */
	public static String RepositorySelectionPage_groupLocation;

	/** */
	public static String RepositorySelectionPage_groupAuthentication;

	/** */
	public static String RepositorySelectionPage_groupConnection;

	/** */
	public static String RepositorySelectionPage_promptURI;

	/** */
	public static String RepositorySelectionPage_promptHost;

	/** */
	public static String RepositorySelectionPage_promptPath;

	/** */
	public static String RepositorySelectionPage_promptUser;

	/** */
	public static String RepositorySelectionPage_promptPassword;

	/** */
	public static String RepositorySelectionPage_promptScheme;

	/** */
	public static String RepositorySelectionPage_promptPort;

	/** */
	public static String RepositorySelectionPage_fieldRequired;

	/** */
	public static String RepositorySelectionPage_fieldNotSupported;

	/** */
	public static String RepositorySelectionPage_fileNotFound;

	/** */
	public static String RepositorySelectionPage_internalError;

	/** */
	public static String RepositorySelectionPage_configuredRemoteChoice;

	/** */
	public static String RepositorySelectionPage_uriChoice;

	/** */
	public static String SourceBranchPage_title;

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
	public static String SourceBranchPage_remoteListingCancelled;

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

	static {
		initializeMessages(UIText.class.getPackage().getName() + ".uitext",
				UIText.class);
	}
}
