package org.spearce.egit.core;

import org.eclipse.osgi.util.NLS;

public class CoreText extends NLS {
    public static String ConnectProviderOperation_connecting;

    public static String ConnectProviderOperation_creating;

    public static String ConnectProviderOperation_recordingMapping;

    public static String DisconnectProviderOperation_disconnecting;

    public static String GitProjectData_mappedResourceGone;

    public static String RepositoryFinder_finding;

    public static String MoveDeleteHook_cannotModifyFolder;

    static {
        NLS.initializeMessages(CoreText.class.getPackage().getName()
                + ".coretext", CoreText.class);
    }
}
