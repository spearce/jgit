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
