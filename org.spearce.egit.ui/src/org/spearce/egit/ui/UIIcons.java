package org.spearce.egit.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

public class UIIcons
{
    public static final ImageDescriptor OVR_PENDING_ADD;

    public static final ImageDescriptor OVR_PENDING_REMOVE;

    public static final ImageDescriptor OVR_SHARED;

    private static final URL base;

    static
    {
        base = init();
        OVR_PENDING_ADD = map("ovr/pending_add.gif");
        OVR_PENDING_REMOVE = map("ovr/pending_remove.gif");
        OVR_SHARED = map("ovr/shared.gif");
    }

    private static ImageDescriptor map(final String icon)
    {
        if (base != null)
        {
            try
            {
                return ImageDescriptor.createFromURL(new URL(base, icon));
            }
            catch (MalformedURLException mux)
            {
                Activator.logError("Can't load plugin image.", mux);
            }
        }
        return ImageDescriptor.getMissingImageDescriptor();
    }

    private static URL init()
    {
        try
        {
            return new URL(
                Activator.getDefault().getBundle().getEntry("/"),
                "icons/");
        }
        catch (MalformedURLException mux)
        {
            Activator.logError("Can't determine icon base.", mux);
            return null;
        }
    }
}
