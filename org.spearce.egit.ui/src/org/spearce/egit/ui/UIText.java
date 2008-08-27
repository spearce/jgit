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
	public static String GitCloneWizard_errorCannotCreate;

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
	public static String SourceBranchPage_description;

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
	public static String SourceBranchPage_cannotListBranches;

	/** */
	public static String SourceBranchPage_remoteListingCancelled;

	/** */
	public static String SourceBranchPage_cannotCreateTemp;

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
	public static String CloneDestinationPage_errorNotEmptyDir;

	/** */
	public static String RefSpecPanel_refChooseSome;

	/** */
	public static String RefSpecPanel_refChooseSomeWildcard;

	/** */
	public static String RefSpecPanel_refChooseRemoteName;

	/** */
	public static String RefSpecPanel_clickToChange;

	/** */
	public static String RefSpecPanel_columnDst;

	/** */
	public static String RefSpecPanel_columnForce;

	/** */
	public static String RefSpecPanel_columnMode;

	/** */
	public static String RefSpecPanel_columnRemove;

	/** */
	public static String RefSpecPanel_columnSrc;

	/** */
	public static String RefSpecPanel_creationButton;

	/** */
	public static String RefSpecPanel_creationButtonDescription;

	/** */
	public static String RefSpecPanel_creationDst;

	/** */
	public static String RefSpecPanel_creationGroup;

	/** */
	public static String RefSpecPanel_creationSrc;

	/** */
	public static String RefSpecPanel_deletionButton;

	/** */
	public static String RefSpecPanel_deletionButtonDescription;

	/** */
	public static String RefSpecPanel_deletionGroup;

	/** */
	public static String RefSpecPanel_deletionRef;

	/** */
	public static String RefSpecPanel_dstDeletionDescription;

	/** */
	public static String RefSpecPanel_dstFetchDescription;

	/** */
	public static String RefSpecPanel_dstPushDescription;

	/** */
	public static String RefSpecPanel_errorRemoteConfigDescription;

	/** */
	public static String RefSpecPanel_errorRemoteConfigTitle;

	/** */
	public static String RefSpecPanel_fetch;

	/** */
	public static String RefSpecPanel_srcFetchDescription;

	/** */
	public static String RefSpecPanel_forceAll;

	/** */
	public static String RefSpecPanel_forceAllDescription;

	/** */
	public static String RefSpecPanel_forceDeleteDescription;

	/** */
	public static String RefSpecPanel_forceFalseDescription;

	/** */
	public static String RefSpecPanel_forceTrueDescription;

	/** */
	public static String RefSpecPanel_modeDelete;

	/** */
	public static String RefSpecPanel_modeDeleteDescription;

	/** */
	public static String RefSpecPanel_modeUpdate;

	/** */
	public static String RefSpecPanel_modeUpdateDescription;

	/** */
	public static String RefSpecPanel_predefinedAll;

	/** */
	public static String RefSpecPanel_predefinedAllDescription;

	/** */
	public static String RefSpecPanel_predefinedConfigured;

	/** */
	public static String RefSpecPanel_predefinedConfiguredDescription;

	/** */
	public static String RefSpecPanel_predefinedGroup;

	/** */
	public static String RefSpecPanel_predefinedTags;

	/** */
	public static String RefSpecPanel_predefinedTagsDescription;

	/** */
	public static String RefSpecPanel_push;

	/** */
	public static String RefSpecPanel_srcPushDescription;

	/** */
	public static String RefSpecPanel_removeAll;

	/** */
	public static String RefSpecPanel_removeAllDescription;

	/** */
	public static String RefSpecPanel_removeDescription;

	/** */
	public static String RefSpecPanel_specifications;

	/** */
	public static String RefSpecPanel_srcDeleteDescription;

	/** */
	public static String RefSpecPanel_validationDstInvalidExpression;

	/** */
	public static String RefSpecPanel_validationDstRequired;

	/** */
	public static String RefSpecPanel_validationRefDeleteRequired;

	/** */
	public static String RefSpecPanel_validationRefDeleteWildcard;

	/** */
	public static String RefSpecPanel_validationRefInvalidExpression;

	/** */
	public static String RefSpecPanel_validationRefInvalidLocal;

	/** */
	public static String RefSpecPanel_validationRefNonExistingRemote;

	/** */
	public static String RefSpecPanel_validationRefNonExistingRemoteDelete;

	/** */
	public static String RefSpecPanel_validationRefNonMatchingLocal;

	/** */
	public static String RefSpecPanel_validationRefNonMatchingRemote;

	/** */
	public static String RefSpecPanel_validationSpecificationsOverlappingDestination;

	/** */
	public static String RefSpecPanel_validationSrcUpdateRequired;

	/** */
	public static String RefSpecPanel_validationWildcardInconsistent;

	/** */
	public static String RefSpecPage_descriptionFetch;

	/** */
	public static String RefSpecPage_descriptionPush;

	/** */
	public static String RefSpecPage_errorDontMatchSrc;

	/** */
	public static String RefSpecPage_errorTransportDialogMessage;

	/** */
	public static String RefSpecPage_errorTransportDialogTitle;

	/** */
	public static String RefSpecPage_operationCancelled;

	/** */
	public static String RefSpecPage_saveSpecifications;

	/** */
	public static String RefSpecPage_titleFetch;

	/** */
	public static String RefSpecPage_titlePush;

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
	public static String PushAction_wrongURIDescription;

	/** */
	public static String PushAction_wrongURITitle;

	/** */
	public static String PushWizard_cantConnectToAny;

	/** */
	public static String PushWizard_cantPrepareUpdatesMessage;

	/** */
	public static String PushWizard_cantPrepareUpdatesTitle;

	/** */
	public static String PushWizard_cantSaveMessage;

	/** */
	public static String PushWizard_cantSaveTitle;

	/** */
	public static String PushWizard_jobName;

	/** */
	public static String PushWizard_missingRefsMessage;

	/** */
	public static String PushWizard_missingRefsTitle;

	/** */
	public static String PushWizard_unexpectedError;

	/** */
	public static String PushWizard_windowTitleDefault;

	/** */
	public static String PushWizard_windowTitleWithDestination;

	/** */
	public static String ConfirmationPage_cantConnectToAny;

	/** */
	public static String ConfirmationPage_description;

	/** */
	public static String ConfirmationPage_errorCantResolveSpecs;

	/** */
	public static String ConfirmationPage_errorInterrupted;

	/** */
	public static String ConfirmationPage_errorRefsChangedNoMatch;

	/** */
	public static String ConfirmationPage_errorUnexpected;

	/** */
	public static String ConfirmationPage_requireUnchangedButton;

	/** */
	public static String ConfirmationPage_showOnlyIfChanged;

	/** */
	public static String ConfirmationPage_title;

	/** */
	public static String PushResultTable_columnStatusRepo;

	/** */
	public static String PushResultTable_columnDst;

	/** */
	public static String PushResultTable_columnSrc;

	/** */
	public static String PushResultTable_columnMode;

	/** */
	public static String PushResultTable_statusUnexpected;

	/** */
	public static String PushResultTable_statusConnectionFailed;

	/** */
	public static String PushResultTable_statusDetailChanged;

	/** */
	public static String PushResultTable_refNonExisting;

	/** */
	public static String PushResultTable_statusDetailDeleted;

	/** */
	public static String PushResultTable_statusDetailNonFastForward;

	/** */
	public static String PushResultTable_statusDetailNoDelete;

	/** */
	public static String PushResultTable_statusDetailNonExisting;

	/** */
	public static String PushResultTable_statusDetailForcedUpdate;

	/** */
	public static String PushResultTable_statusDetailFastForward;

	/** */
	public static String PushResultTable_statusRemoteRejected;

	/** */
	public static String PushResultTable_statusRejected;

	/** */
	public static String PushResultTable_statusNoMatch;

	/** */
	public static String PushResultTable_statusUpToDate;

	/** */
	public static String PushResultTable_statusOkDeleted;

	/** */
	public static String PushResultTable_statusOkNewBranch;

	/** */
	public static String PushResultTable_statusOkNewTag;

	/** */
	public static String ResultDialog_title;

	/** */
	public static String ResultDialog_label;

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
