/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.spearce.egit.core.project.GitProjectData;

/**
 * The plugin class for the org.spearce.egit.core plugin. This
 * is a singleton class.
 */
public class Activator extends AbstractUIPlugin {
	private static Activator plugin;

	/**
	 * @return the singleton {@link Activator}
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * @return the name of this plugin
	 */
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	/**
	 * Utility method to help throwing errors in the Egit plugin. This method
	 * does not actually throw the exception, but just creates an instance.
	 *
	 * @param message User comprehensible message
	 * @param thr cause
	 * @return an Initialized {@link CoreException}
	 */
	public static CoreException error(final String message, final Throwable thr) {
		return new CoreException(new Status(IStatus.ERROR, getPluginId(), 0,
				message, thr));
	}

	/**
	 * Utility method to log errors in the Egit plugin.
	 * @param message User comprehensible message
	 * @param thr The exception through which we noticed the error
	 */
	public static void logError(final String message, final Throwable thr) {
		getDefault().getLog().log(
				new Status(IStatus.ERROR, getPluginId(), 0, message, thr));
	}

	private static boolean isOptionSet(final String optionId) {
		final String option = getPluginId() + optionId;
		final String value = Platform.getDebugOption(option);
		return value != null && value.equals("true");
	}

	/**
	 * Utility method for debug logging.
	 *
	 * @param what
	 */
	public static void trace(final String what) {
		if (getDefault().traceVerbose) {
			System.out.println("[" + getPluginId() + "] " + what);
		}
	}

	private boolean traceVerbose;

	/**
	 * Construct the {@link Activator} singleton instance
	 */
	public Activator() {
		plugin = this;
	}

	public void start(final BundleContext context) throws Exception {
		super.start(context);
		traceVerbose = isOptionSet("/trace/verbose");
		GitProjectData.reconfigureWindowCache();
		GitProjectData.attachToWorkspace(true);
	}

	public void stop(final BundleContext context) throws Exception {
		GitProjectData.detachFromWorkspace();
		super.stop(context);
		plugin = null;
	}
}
