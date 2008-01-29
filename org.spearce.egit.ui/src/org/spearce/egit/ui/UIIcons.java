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

/**
 * Icons for the the Eclipse plugin. Mostly decorations.
 */
public class UIIcons {
	/** Decoration for resource added to index but not yet committed. */
	public static final ImageDescriptor OVR_PENDING_ADD;

	/** Decoratoin for resource removed from the index but not commit. */
	public static final ImageDescriptor OVR_PENDING_REMOVE;

	/** Decoration for resource tracked and commited in git. */
	public static final ImageDescriptor OVR_SHARED;

	/** Decoration for tracked resource with a merge conflict.  */
	public static final ImageDescriptor OVR_CONFLICT;

	/** Decoration for tracked resources that we want to ignore changes in. */
	public static final ImageDescriptor OVR_ASSUMEVALID;

	/** Find icon */
	public static final ImageDescriptor ELCL16_FIND;
	/** Next arrow icon */
	public static final ImageDescriptor ELCL16_NEXT;
	/** Previous arrow icon */
	public static final ImageDescriptor ELCL16_PREVIOUS;

	private static final URL base;

	static {
		base = init();
		OVR_PENDING_ADD = map("ovr/pending_add.gif");
		OVR_PENDING_REMOVE = map("ovr/pending_remove.gif");
		OVR_SHARED = map("ovr/shared.gif");
		OVR_CONFLICT = map("ovr/conflict.gif");
		OVR_ASSUMEVALID = map("ovr/assumevalid.gif");
		ELCL16_FIND = map("elcl16/find.gif");
		ELCL16_NEXT = map("elcl16/next.gif");
		ELCL16_PREVIOUS = map("elcl16/previous.gif");
	}

	private static ImageDescriptor map(final String icon) {
		if (base != null) {
			try {
				return ImageDescriptor.createFromURL(new URL(base, icon));
			} catch (MalformedURLException mux) {
				Activator.logError("Can't load plugin image.", mux);
			}
		}
		return ImageDescriptor.getMissingImageDescriptor();
	}

	private static URL init() {
		try {
			return new URL(Activator.getDefault().getBundle().getEntry("/"),
					"icons/");
		} catch (MalformedURLException mux) {
			Activator.logError("Can't determine icon base.", mux);
			return null;
		}
	}
}
