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
package org.spearce.egit.core;

import org.eclipse.osgi.util.NLS;

public class CoreText extends NLS {
	public static String AssumeUnchangedOperation_failed;

	public static String AssumeUnchangedOperation_adding;

	public static String UpdateOperation_updating;

	public static String UpdateOperation_failed;

	public static String ConnectProviderOperation_connecting;

	public static String ConnectProviderOperation_creating;

	public static String ConnectProviderOperation_recordingMapping;

	public static String ConnectProviderOperation_updatingCache;

	public static String DisconnectProviderOperation_disconnecting;

	public static String AddOperation_adding;

	public static String AddOperation_failed;

	public static String UntrackOperation_adding;

	public static String UntrackOperation_failed;

	public static String GitProjectData_lazyResolveFailed;

	public static String GitProjectData_mappedResourceGone;

	public static String GitProjectData_cannotReadHEAD;

	public static String GitProjectData_missing;

	public static String GitProjectData_saveFailed;

	public static String GitProjectData_notifyChangedFailed;

	public static String RepositoryFinder_finding;

	public static String MoveDeleteHook_cannotModifyFolder;

	public static String MoveDeleteHook_operationError;

	public static String Error_CanonicalFile;

	public static String CheckpointJob_writing;

	public static String CheckpointJob_name;

	public static String CheckpointJob_failed;

	static {
		final Class c = CoreText.class;
		initializeMessages(c.getPackage().getName() + ".coretext", c);
	}
}
