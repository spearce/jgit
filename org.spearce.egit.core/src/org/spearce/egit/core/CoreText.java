/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core;

import org.eclipse.osgi.util.NLS;

/**
 * Possibly Translated strings for the Egit plugin.
 */
public class CoreText extends NLS {

	/** */
	public static String AssumeUnchangedOperation_adding;

	/** */
	public static String UpdateOperation_updating;

	/** */
	public static String UpdateOperation_failed;

	/** */
	public static String ConnectProviderOperation_connecting;

	/** */
	public static String ConnectProviderOperation_ConnectingProject;

	/** */
	public static String DisconnectProviderOperation_disconnecting;

	/** */
	public static String AddOperation_adding;

	/** */
	public static String AddOperation_failed;

	/** */
	public static String UntrackOperation_adding;

	/** */
	public static String UntrackOperation_failed;

	/** */
	public static String GitProjectData_lazyResolveFailed;

	/** */
	public static String GitProjectData_mappedResourceGone;

	/** */
	public static String GitProjectData_cannotReadHEAD;

	/** */
	public static String GitProjectData_missing;

	/** */
	public static String GitProjectData_saveFailed;

	/** */
	public static String GitProjectData_notifyChangedFailed;

	/** */
	public static String RepositoryFinder_finding;

	/** */
	public static String MoveDeleteHook_cannotModifyFolder;

	/** */
	public static String MoveDeleteHook_operationError;

	/** */
	public static String Error_CanonicalFile;

	/** */
	public static String CloneOperation_title;

	/** */
	public static String ListRemoteOperation_title;

	/** */
	public static String PushOperation_resultCancelled;

	/** */
	public static String PushOperation_resultNotSupported;

	/** */
	public static String PushOperation_resultTransportError;

	/** */
	public static String PushOperation_resultNoServiceError;

	/** */
	public static String PushOperation_taskNameDryRun;

	/** */
	public static String PushOperation_taskNameNormalRun;

	static {
		initializeMessages("org.spearce.egit.core.coretext", CoreText.class);
	}
}
