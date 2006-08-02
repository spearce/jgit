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
