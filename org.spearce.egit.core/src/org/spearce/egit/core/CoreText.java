/*
 *    Copyright 2006 Shawn Pearce <spearce@spearce.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearce.egit.core;

import org.eclipse.osgi.util.NLS;

public class CoreText extends NLS
{
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

    public static String CheckpointJob_writingBlobs;

    public static String CheckpointJob_writingTrees;

    public static String CheckpointJob_writingRef;

    public static String CheckpointJob_failed;

    static
    {
        final Class c = CoreText.class;
        initializeMessages(c.getPackage().getName() + ".coretext", c);
    }
}
