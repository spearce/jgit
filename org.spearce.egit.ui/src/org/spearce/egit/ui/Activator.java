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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.spearce.egit.core.GitProvider;

public class Activator extends AbstractUIPlugin {
    private static Activator plugin;

    public static Activator getDefault() {
	return plugin;
    }

    public static String getPluginId() {
	return getDefault().getBundle().getSymbolicName();
    }

    /**
         * This is the provider ID of the plugin as defined in the plugin.xml
         * FIXME Inspired/Copied from FileSystem example. Specialize to EGit
         */
    public static String getPluginProviderId() {
	return GitProvider.class.getName();
    }

    /**
         * Convenience method to get the currently active workbench page. Note
         * that the active page may not be the one that the usr perceives as
         * active in some situations so this method of obtaining the activae
         * page should only be used if no other method is available.
         * 
         * @return the active workbench page
         */
    public static IWorkbenchPage getActivePage() {
	IWorkbenchWindow window = getDefault().getWorkbench()
		.getActiveWorkbenchWindow();
	if (window == null)
	    return null;
	return window.getActivePage();
    }

    public static CoreException error(final String message, final Throwable thr) {
	return new CoreException(new Status(IStatus.ERROR, getPluginId(), 0,
		message, thr));
    }

    public static void logError(final String message, final Throwable thr) {
	getDefault().getLog().log(
		new Status(IStatus.ERROR, getPluginId(), 0, message, thr));
    }

    public static void log(TeamException e) {
	getDefault().getLog().log(
		new Status(e.getStatus().getSeverity(), getPluginId(), 0,
			"simpleInternal", e)); //$NON-NLS-1$
    }

    private static boolean isOptionSet(final String optionId) {
	final String option = getPluginId() + optionId;
	final String value = Platform.getDebugOption(option);
	return value != null && value.equals("true");
    }

    public static void trace(final String what) {
	if (getDefault().traceVerbose) {
	    System.out.println("[" + getPluginId() + "] " + what);
	}
    }

    private boolean traceVerbose;

    public Activator() {
	plugin = this;
    }

    public void start(final BundleContext context) throws Exception {
	super.start(context);
	traceVerbose = isOptionSet("/trace/verbose");
    }

    public void stop(final BundleContext context) throws Exception {
	super.stop(context);
	plugin = null;
    }
}
