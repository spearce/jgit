/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Icons for the the Eclipse plugin. Mostly decorations.
 */
public class UIIcons {

	/** Decoration for resource in the index but not yet committed. */
	public static final ImageDescriptor OVR_STAGED;

	/** Decoration for resource added to index but not yet committed. */
	public static final ImageDescriptor OVR_STAGED_ADD;

	/** Decoration for resource removed from the index but not commit. */
	public static final ImageDescriptor OVR_STAGED_REMOVE;

	/** Decoration for resource not being tracked by Git */
	public static final ImageDescriptor OVR_UNTRACKED;

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
	/** Commit icon */
	public static final ImageDescriptor ELCL16_COMMIT;
	/** Comments icon */
	public static final ImageDescriptor ELCL16_COMMENTS;
	/** Author icon */
	public static final ImageDescriptor ELCL16_AUTHOR;
	/** Committer icon */
	public static final ImageDescriptor ELCL16_COMMITTER;
	/** Delete icon */
	public static final ImageDescriptor ELCL16_DELETE;
	/** Add icon */
	public static final ImageDescriptor ELCL16_ADD;
	/** Trash icon */
	public static final ImageDescriptor ELCL16_TRASH;
	/** Clear icon */
	public static final ImageDescriptor ELCL16_CLEAR;

	/** Enabled, checked, checkbox image */
	public static final ImageDescriptor CHECKBOX_ENABLED_CHECKED;
	/** Enabled, unchecked, checkbox image */
	public static final ImageDescriptor CHECKBOX_ENABLED_UNCHECKED;
	/** Disabled, checked, checkbox image */
	public static final ImageDescriptor CHECKBOX_DISABLED_CHECKED;
	/** Disabled, unchecked, checkbox image */
	public static final ImageDescriptor CHECKBOX_DISABLED_UNCHECKED;

	/** Import Wizard banner */
	public static final ImageDescriptor WIZBAN_IMPORT_REPO;

	/** Connect Wizard banner */
	public static final ImageDescriptor WIZBAN_CONNECT_REPO;

	/** History filter, select all version in repo */
	public static ImageDescriptor FILTERREPO;

	/** History filter, select all version in same project */
	public static ImageDescriptor FILTERPROJECT;

	/** History filter, select all version in same folder */
	public static ImageDescriptor FILTERFOLDER;

	private static final URL base;

	static {
		base = init();
		OVR_STAGED = map("ovr/staged.gif");
		OVR_STAGED_ADD = map("ovr/staged_added.gif");
		OVR_STAGED_REMOVE = map("ovr/staged_removed.gif");
		OVR_UNTRACKED = map("ovr/untracked.gif");
		OVR_CONFLICT = map("ovr/conflict.gif");
		OVR_ASSUMEVALID = map("ovr/assume_valid.gif");
		ELCL16_FIND = map("elcl16/find.gif");
		ELCL16_NEXT = map("elcl16/next.gif");
		ELCL16_PREVIOUS = map("elcl16/previous.gif");
		WIZBAN_IMPORT_REPO = map("wizban/import_wiz.png");
		WIZBAN_CONNECT_REPO = map("wizban/newconnect_wizban.png");
		ELCL16_COMMIT = map("elcl16/commit.gif");
		ELCL16_COMMENTS = map("elcl16/comment.gif");
		ELCL16_AUTHOR = map("elcl16/author.gif");
		ELCL16_COMMITTER = map("elcl16/committer.gif");
		ELCL16_DELETE = map("elcl16/delete.gif");
		ELCL16_ADD = map("elcl16/add.gif");
		ELCL16_TRASH = map("elcl16/trash.gif");
		ELCL16_CLEAR = map("elcl16/clear.gif");
		CHECKBOX_ENABLED_CHECKED = map("checkboxes/enabled_checked.gif");
		CHECKBOX_ENABLED_UNCHECKED = map("checkboxes/enabled_unchecked.gif");
		CHECKBOX_DISABLED_CHECKED = map("checkboxes/disabled_checked.gif");
		CHECKBOX_DISABLED_UNCHECKED = map("checkboxes/disabled_unchecked.gif");
		FILTERREPO = map("elcl16/filterrepo.gif");
		FILTERPROJECT = map("elcl16/filterproject.gif");
		FILTERFOLDER = map("elcl16/filterfolder.gif");
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
