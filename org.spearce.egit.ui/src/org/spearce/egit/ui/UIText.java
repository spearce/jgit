package org.spearce.egit.ui;

import java.text.MessageFormat;

import org.eclipse.osgi.util.NLS;

public class UIText extends NLS {
    public static String SharingWizard_windowTitle;

    public static String SharingWizard_failed;

    public static String GenericOperationFailed;

    public static String ExistingOrNewPage_title;

    public static String ExistingOrNewPage_description;

    public static String ExistingOrNewPage_groupHeader;

    public static String ExistingOrNewPage_useExisting;

    public static String ExistingOrNewPage_createNew;

    public static String format_GenericOperationFailed(final String opName) {
        return new MessageFormat(GenericOperationFailed)
                .format(new Object[] { opName });
    }

    static {
        NLS.initializeMessages(UIText.class.getPackage().getName() + ".uitext",
                UIText.class);
    }
}
