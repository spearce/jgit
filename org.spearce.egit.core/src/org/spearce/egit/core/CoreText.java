package org.spearce.egit.core;

import org.eclipse.osgi.util.NLS;

public class CoreText extends NLS {
    public static String RegisterProviderJob_name;

    static {
        NLS.initializeMessages(CoreText.class.getPackage().getName()
                + ".coretext", CoreText.class);
    }
}
