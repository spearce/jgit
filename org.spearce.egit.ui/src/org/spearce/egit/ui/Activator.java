/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui;

import java.net.Authenticator;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.themes.ITheme;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.IndexChangedEvent;
import org.spearce.jgit.lib.RefsChangedEvent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryListener;
import org.spearce.jgit.transport.SshSessionFactory;

/**
 * This is a plugin singleton mostly controlling logging.
 */
public class Activator extends AbstractUIPlugin {

	/**
	 *  The one and only instance
	 */
	private static Activator plugin;

	/**
	 * Property listeners for plugin specific events
	 */
	private static List<IPropertyChangeListener> propertyChangeListeners =
		new ArrayList<IPropertyChangeListener>(5);

	/**
	 * Property constant indicating the decorator configuration has changed.
	 */
	public static final String DECORATORS_CHANGED = "org.spearce.egit.ui.DECORATORS_CHANGED"; //$NON-NLS-1$

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
	 * Returns the standard display to be used. The method first checks, if the
	 * thread calling this method has an associated display. If so, this display
	 * is returned. Otherwise the method returns the default display.
	 *
	 * @return the display to use
	 */
	public static Display getStandardDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
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
	private RCS rcs;
	private RIRefresh refreshJob;

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
		setupRepoChangeScanner();
		setupRepoIndexRefresh();
	}

	private void setupRepoIndexRefresh() {
		refreshJob = new RIRefresh();
		Repository.addAnyRepositoryChangedListener(refreshJob);
	}

	/**
	 * Register for changes made to Team properties.
	 *
	 * @param listener
	 *            The listener to register
	 */
	public static synchronized void addPropertyChangeListener(
			IPropertyChangeListener listener) {
		propertyChangeListeners.add(listener);
	}

	/**
	 * Remove a Team property changes.
	 *
	 * @param listener
	 *            The listener to remove
	 */
	public static synchronized void removePropertyChangeListener(
			IPropertyChangeListener listener) {
		propertyChangeListeners.remove(listener);
	}

	/**
	 * Broadcast a Team property change.
	 *
	 * @param event
	 *            The event to broadcast
	 */
	public static synchronized void broadcastPropertyChange(PropertyChangeEvent event) {
		for (IPropertyChangeListener listener : propertyChangeListeners)
			listener.propertyChange(event);
	}

	static class RIRefresh extends Job implements RepositoryListener {

		RIRefresh() {
			super("Git index refresh Job");
		}

		private Set<IProject> projectsToScan = new LinkedHashSet<IProject>();

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			monitor.beginTask("Refreshing git managed projects", projects.length);

			while (projectsToScan.size() > 0) {
				IProject p;
				synchronized (projectsToScan) {
					if (projectsToScan.size() == 0)
						break;
					Iterator<IProject> i = projectsToScan.iterator();
					p = i.next();
					i.remove();
				}
				ISchedulingRule rule = p.getWorkspace().getRuleFactory().refreshRule(p);
				try {
					getJobManager().beginRule(rule, monitor);
					p.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					logError("Failed to refresh projects from index changes", e);
					return new Status(IStatus.ERROR, getPluginId(), e.getMessage());
				} finally {
					getJobManager().endRule(rule);
				}
			}
			monitor.done();
			return Status.OK_STATUS;
		}

		public void indexChanged(IndexChangedEvent e) {
			// Check the workspace setting "refresh automatically" setting first
			if (!ResourcesPlugin.getPlugin().getPluginPreferences().getBoolean(
					ResourcesPlugin.PREF_AUTO_REFRESH))
				return;

			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			Set<IProject> toRefresh= new HashSet<IProject>();
			for (IProject p : projects) {
				RepositoryMapping mapping = RepositoryMapping.getMapping(p);
				if (mapping != null && mapping.getRepository() == e.getRepository()) {
					toRefresh.add(p);
				}
			}
			synchronized (projectsToScan) {
				projectsToScan.addAll(toRefresh);
			}
			if (projectsToScan.size() > 0)
				schedule();
		}

		public void refsChanged(RefsChangedEvent e) {
			// Do not react here
		}

	}

	static class RCS extends Job {
		RCS() {
			super("Repository Change Scanner");
		}

		// FIXME, need to be more intelligent about this to avoid too much work
		private static final long REPO_SCAN_INTERVAL = 10000L;

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				// A repository can contain many projects, only scan once
				// (a project could in theory be distributed among many
				// repositories. We discard that as being ugly and stupid for
				// the moment.
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				monitor.beginTask("Scanning Git repositories for changes", projects.length);
				Set<Repository> scanned = new HashSet<Repository>();
				for (IProject p : projects) {
					RepositoryMapping mapping = RepositoryMapping.getMapping(p);
					if (mapping != null) {
						Repository r = mapping.getRepository();
						if (!scanned.contains(r)) {
							if (monitor.isCanceled())
								break;
							trace("Scanning " + r + " for changes");
							scanned.add(r);
							ISchedulingRule rule = p.getWorkspace().getRuleFactory().modifyRule(p);
							getJobManager().beginRule(rule, monitor);
							try {
								r.scanForRepoChanges();
							} finally {
								getJobManager().endRule(rule);
							}
						}
					}
					monitor.worked(1);
				}
				monitor.done();
				trace("Rescheduling " + getName() + " job");
				schedule(REPO_SCAN_INTERVAL);
			} catch (Exception e) {
				trace("Stopped rescheduling " + getName() + "job");
				return new Status(
						IStatus.ERROR,
						getPluginId(),
						0,
						"An error occurred while scanning for changes. Scanning aborted",
						e);
			}
			return Status.OK_STATUS;
		}
	}

	private void setupRepoChangeScanner() {
		rcs = new RCS();
		rcs.schedule(RCS.REPO_SCAN_INTERVAL);
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
		trace("Trying to cancel " + rcs.getName() + " job");
		rcs.cancel();
		trace("Trying to cancel " + refreshJob.getName() + " job");
		refreshJob.cancel();

		rcs.join();
		refreshJob.join();

		trace("Jobs terminated");
		super.stop(context);
		plugin = null;
	}
}
