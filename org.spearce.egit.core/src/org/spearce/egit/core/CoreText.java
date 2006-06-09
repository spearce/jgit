package org.spearce.egit.core;

import org.eclipse.osgi.util.NLS;

public class CoreText extends NLS
{
    public static String ConnectProviderOperation_connecting;

    public static String ConnectProviderOperation_creating;

    public static String ConnectProviderOperation_recordingMapping;

    public static String DisconnectProviderOperation_disconnecting;

    public static String GitProjectData_mappedResourceGone;

    public static String GitProjectData_missing;

    public static String GitProjectData_saveFailed;

    public static String RepositoryFinder_finding;

    public static String MoveDeleteHook_cannotModifyFolder;

    public static String Error_CanonicalFile;

    static
    {
        final Class c = CoreText.class;
        initializeMessages(c.getPackage().getName() + ".coretext", c);
    }
}
