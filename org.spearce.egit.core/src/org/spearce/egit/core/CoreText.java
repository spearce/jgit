package org.spearce.egit.core;

import org.eclipse.osgi.util.NLS;

public class CoreText extends NLS {
    public static String ConnectProviderOperation_creating;

    public static String ConnectProviderOperation_failed;

    static {
        NLS.initializeMessages(CoreText.class.getPackage().getName()
                + ".coretext", CoreText.class);
    }
}
