package org.spearce.egit.ui;

import org.eclipse.osgi.util.NLS;

public class UIText extends NLS
{
    public static String SharingWizard_windowTitle;

    public static String SharingWizard_failed;

    public static String GenericOperationFailed;

    public static String ExistingOrNewPage_title;

    public static String ExistingOrNewPage_description;

    public static String ExistingOrNewPage_groupHeader;

    public static String ExistingOrNewPage_useExisting;

    public static String ExistingOrNewPage_createNew;

    public static String Decorator_failedLazyLoading;
    
    static
    {
        initializeMessages(
            UIText.class.getPackage().getName() + ".uitext",
            UIText.class);
    }
}
