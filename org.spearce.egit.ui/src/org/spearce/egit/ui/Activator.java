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

import java.net.Authenticator;
import java.net.ProxySelector;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.themes.ITheme;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.spearce.jgit.transport.SshSessionFactory;

/**
 * This is a plugin singleton mostly controlling logging.
 */
public class Activator extends AbstractUIPlugin {
	private static Activator plugin;

	/**
	 * @return the {@link Activator} singleton.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * @return the id of the egit ui plugin
	 */
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	/**
	 * Instantiate an error exception.
	 * 
	 * @param message
	 *            description of the error
	 * @param thr
	 *            cause of the error or null
	 * @return an initialized {@link CoreException}
	 */
	public static CoreException error(final String message, final Throwable thr) {
		return new CoreException(new Status(IStatus.ERROR, getPluginId(), 0,
				message, thr));
	}

	/**
	 * Log an error via the Eclipse logging routines.
	 * 
	 * @param message
	 * @param thr
	 *            cause of error
	 */
	public static void logError(final String message, final Throwable thr) {
		getDefault().getLog().log(
				new Status(IStatus.ERROR, getPluginId(), 0, message, thr));
	}

	/**
	 * @param optionId
	 *            name of debug option
	 * @return whether a named debug option is set
	 */
	private static boolean isOptionSet(final String optionId) {
		final String option = getPluginId() + optionId;
		final String value = Platform.getDebugOption(option);
		return value != null && value.equals("true");
	}

	/**
	 * Log a debug message
	 * 
	 * @param what
	 *            message to log
	 */
	public static void trace(final String what) {
		if (getDefault().traceVerbose) {
			System.out.println("[" + getPluginId() + "] " + what);
		}
	}

	/**
	 * Get the theme used by this plugin.
	 * 
	 * @return our theme.
	 */
	public static ITheme getTheme() {
		return plugin.getWorkbench().getThemeManager().getCurrentTheme();
	}

	/**
	 * Get a font known to this plugin.
	 * 
	 * @param id
	 *            one of our THEME_* font preference ids (see
	 *            {@link UIPreferences});
	 * @return the configured font, borrowed from the registry.
	 */
	public static Font getFont(final String id) {
		return getTheme().getFontRegistry().get(id);
	}

	/**
	 * Get a font known to this plugin, but with bold style applied over top.
	 * 
	 * @param id
	 *            one of our THEME_* font preference ids (see
	 *            {@link UIPreferences});
	 * @return the configured font, borrowed from the registry.
	 */
	public static Font getBoldFont(final String id) {
		return getTheme().getFontRegistry().getBold(id);
	}

	private boolean traceVerbose;

	/**
	 * Constructor for the egit ui plugin singleton
	 */
	public Activator() {
		plugin = this;
	}

	public void start(final BundleContext context) throws Exception {
		super.start(context);
		traceVerbose = isOptionSet("/trace/verbose");
		setupSSH(context);
		setupProxy(context);
	}

	private void setupSSH(final BundleContext context) {
		final ServiceReference ssh;

		ssh = context.getServiceReference(IJSchService.class.getName());
		if (ssh != null) {
			SshSessionFactory.setInstance(new EclipseSshSessionFactory(
					(IJSchService) context.getService(ssh)));
		}
	}

	private void setupProxy(final BundleContext context) {
		final ServiceReference proxy;

		proxy = context.getServiceReference(IProxyService.class.getName());
		if (proxy != null) {
			ProxySelector.setDefault(new EclipseProxySelector(
					(IProxyService) context.getService(proxy)));
			Authenticator.setDefault(new EclipseAuthenticator(
					(IProxyService) context.getService(proxy)));
		}
	}

	public void stop(final BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}
}
