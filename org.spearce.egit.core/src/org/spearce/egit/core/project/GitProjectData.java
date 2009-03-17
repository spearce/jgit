/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.GitCorePreferences;
import org.spearce.egit.core.GitProvider;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.WindowCache;
import org.spearce.jgit.lib.WindowCacheConfig;

/**
 * This class keeps information about how a project is mapped to
 * a Git repository.
 */
public class GitProjectData {
	private static final Map<IProject, GitProjectData> projectDataCache = new HashMap<IProject, GitProjectData>();

	private static final Map<File, WeakReference> repositoryCache = new HashMap<File, WeakReference>();

	private static Set<RepositoryChangeListener> repositoryChangeListeners = new HashSet<RepositoryChangeListener>();

	@SuppressWarnings("synthetic-access")
	private static final IResourceChangeListener rcl = new RCL();

	private static class RCL implements IResourceChangeListener {
		@SuppressWarnings("synthetic-access")
		public void resourceChanged(final IResourceChangeEvent event) {
			switch (event.getType()) {
			case IResourceChangeEvent.PRE_CLOSE:
				uncache((IProject) event.getResource());
				break;
			case IResourceChangeEvent.PRE_DELETE:
				delete((IProject) event.getResource());
				break;
			default:
				break;
			}
		}
	}

	private static QualifiedName MAPPING_KEY = new QualifiedName(
			GitProjectData.class.getName(), "RepositoryMapping");

	/**
	 * Start listening for resource changes.
	 *
	 * @param includeChange true to listen to content changes
	 */
	public static void attachToWorkspace(final boolean includeChange) {
		trace("attachToWorkspace - addResourceChangeListener");
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				rcl,
				(includeChange ? IResourceChangeEvent.POST_CHANGE : 0)
						| IResourceChangeEvent.PRE_CLOSE
						| IResourceChangeEvent.PRE_DELETE);
	}

	/**
	 * Stop listening to resource changes
	 */
	public static void detachFromWorkspace() {
		trace("detachFromWorkspace - removeResourceChangeListener");
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(rcl);
	}

	/**
	 * Register a new listener for repository modification events.
	 * <p>
	 * This is a no-op if <code>objectThatCares</code> has already been
	 * registered.
	 * </p>
	 * 
	 * @param objectThatCares
	 *            the new listener to register. Must not be null.
	 */
	public static synchronized void addRepositoryChangeListener(
			final RepositoryChangeListener objectThatCares) {
		if (objectThatCares == null)
			throw new NullPointerException();
		repositoryChangeListeners.add(objectThatCares);
	}

	/**
	 * Remove a registered {@link RepositoryChangeListener}
	 *
	 * @param objectThatCares
	 *            The listener to remove
	 */
	public static synchronized void removeRepositoryChangeListener(
			final RepositoryChangeListener objectThatCares) {
		repositoryChangeListeners.remove(objectThatCares);
	}

	/**
	 * Notify registered {@link RepositoryChangeListener}s of a change.
	 * 
	 * @param which
	 *            the repository which has had changes occur within it.
	 */
	static void fireRepositoryChanged(final RepositoryMapping which) {
		for (RepositoryChangeListener listener : getRepositoryChangeListeners())
			listener.repositoryChanged(which);
	}

	/**
	 * Get a copy of the current set of repository change listeners
	 * <p>
	 * The array has no references, so is safe for iteration and modification
	 *
	 * @return a copy of the current repository change listeners
	 */
	private static synchronized RepositoryChangeListener[] getRepositoryChangeListeners() {
		return repositoryChangeListeners
				.toArray(new RepositoryChangeListener[repositoryChangeListeners
						.size()]);
	}

	/**
	 * @param p
	 * @return {@link GitProjectData} for the specified project
	 */
	public synchronized static GitProjectData get(final IProject p) {
		try {
			GitProjectData d = lookup(p);
			if (d == null
					&& RepositoryProvider.getProvider(p) instanceof GitProvider) {
				d = new GitProjectData(p).load();
				cache(p, d);
			}
			return d;
		} catch (IOException err) {
			Activator.logError(CoreText.GitProjectData_missing, err);
			return null;
		}
	}

	/**
	 * Drop the Eclipse project from our association of projects/repositories
	 *
	 * @param p Eclipse project
	 */
	public static void delete(final IProject p) {
		trace("delete(" + p.getName() + ")");
		GitProjectData d = lookup(p);
		if (d == null) {
			try {
				d = new GitProjectData(p).load();
			} catch (IOException ioe) {
				d = new GitProjectData(p);
			}
		}
		d.delete();
	}

	static void trace(final String m) {
		Activator.trace("(GitProjectData) " + m);
	}

	private synchronized static void cache(final IProject p,
			final GitProjectData d) {
		projectDataCache.put(p, d);
	}

	private synchronized static void uncache(final IProject p) {
		if (projectDataCache.remove(p) != null) {
			trace("uncacheDataFor(" + p.getName() + ")");
		}
	}

	private synchronized static GitProjectData lookup(final IProject p) {
		return projectDataCache.get(p);
	}

	private synchronized static Repository lookupRepository(final File gitDir)
			throws IOException {
		final Iterator i = repositoryCache.entrySet().iterator();
		while (i.hasNext()) {
			final Map.Entry e = (Map.Entry) i.next();
			if (((Reference) e.getValue()).get() == null) {
				i.remove();
			}
		}

		final Reference r = repositoryCache.get(gitDir);
		Repository d = r != null ? (Repository) r.get() : null;
		if (d == null) {
			d = new Repository(gitDir);
			repositoryCache.put(gitDir, new WeakReference<Repository>(d));
		}
		return d;
	}

	/**
	 * Update the settings for the global window cache of the workspace.
	 */
	public static void reconfigureWindowCache() {
		final WindowCacheConfig c = new WindowCacheConfig();
		Preferences p = Activator.getDefault().getPluginPreferences();
		c.setPackedGitLimit(p.getInt(GitCorePreferences.core_packedGitLimit));
		c.setPackedGitWindowSize(p.getInt(GitCorePreferences.core_packedGitWindowSize));
		c.setPackedGitMMAP(p.getBoolean(GitCorePreferences.core_packedGitMMAP));
		c.setDeltaBaseCacheLimit(p.getInt(GitCorePreferences.core_deltaBaseCacheLimit));
		WindowCache.reconfigure(c);
	}

	private final IProject project;

	private final Collection<RepositoryMapping> mappings = new ArrayList<RepositoryMapping>();

	private final Set<IResource> protectedResources = new HashSet<IResource>();

	/**
	 * Construct a {@link GitProjectData} for the mapping
	 * of a project.
	 *
	 * @param p Eclipse project
	 */
	public GitProjectData(final IProject p) {
		project = p;
	}

	/**
	 * @return the Eclipse project mapped through this resource.
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * TODO: is this right?
	 *
	 * @param newMappings
	 */
	public void setRepositoryMappings(final Collection<RepositoryMapping> newMappings) {
		mappings.clear();
		mappings.addAll(newMappings);
		remapAll();
	}

	/**
	 * Hide our private parts from the navigators other browsers.
	 *
	 * @throws CoreException
	 */
	public void markTeamPrivateResources() throws CoreException {
		for (final Object rmObj : mappings) {
			final RepositoryMapping rm = (RepositoryMapping)rmObj;
			final IContainer c = rm.getContainer();
			if (c == null)
				continue; // Not fully mapped yet?

			final IResource dotGit = c.findMember(".git");
			if (dotGit != null) {
				try {
					final Repository r = rm.getRepository();
					final File dotGitDir = dotGit.getLocation().toFile()
							.getCanonicalFile();
					if (dotGitDir.equals(r.getDirectory())) {
						trace("teamPrivate " + dotGit);
						dotGit.setTeamPrivateMember(true);
					}
				} catch (IOException err) {
					throw Activator.error(CoreText.Error_CanonicalFile, err);
				}
			}
		}
	}

	/**
	 * @param f
	 * @return true if a resource is protected in this repository
	 */
	public boolean isProtected(final IResource f) {
		return protectedResources.contains(f);
	}

	/**
	 * @param r any workbench resource contained within this project.
	 * @return the mapping for the specified project
	 */
	public RepositoryMapping getRepositoryMapping(IResource r) {
		try {
			for (; r != null; r = r.getParent()) {
				final RepositoryMapping m;

				if (!r.isAccessible())
					continue;
				m = (RepositoryMapping) r.getSessionProperty(MAPPING_KEY);
				if (m != null)
					return m;
			}
		} catch (CoreException err) {
			Activator.logError("Failed finding RepositoryMapping", err);
		}
		return null;
	}

	private void delete() {
		final File dir = propertyFile().getParentFile();
		final File[] todel = dir.listFiles();
		if (todel != null) {
			for (int k = 0; k < todel.length; k++) {
				if (todel[k].isFile()) {
					todel[k].delete();
				}
			}
		}
		dir.delete();
		trace("deleteDataFor(" + getProject().getName() + ")");
		uncache(getProject());
	}

	/**
	 * Store information about the repository connection in the workspace
	 *
	 * @throws CoreException
	 */
	public void store() throws CoreException {
		final File dat = propertyFile();
		final File tmp;
		boolean ok = false;

		try {
			trace("save " + dat);
			tmp = File.createTempFile("gpd_", ".prop", dat.getParentFile());
			final FileOutputStream o = new FileOutputStream(tmp);
			try {
				final Properties p = new Properties();
				final Iterator i = mappings.iterator();
				while (i.hasNext()) {
					((RepositoryMapping) i.next()).store(p);
				}
				p.store(o, "GitProjectData");
				ok = true;
			} finally {
				o.close();
				if (!ok) {
					tmp.delete();
				}
			}
		} catch (IOException ioe) {
			throw Activator.error(NLS.bind(CoreText.GitProjectData_saveFailed,
					dat), ioe);
		}

		dat.delete();
		if (!tmp.renameTo(dat)) {
			tmp.delete();
			throw Activator.error(NLS.bind(CoreText.GitProjectData_saveFailed,
					dat), null);
		}
	}

	private File propertyFile() {
		return new File(getProject()
				.getWorkingLocation(Activator.getPluginId()).toFile(),
				"GitProjectData.properties");
	}

	private GitProjectData load() throws IOException {
		final File dat = propertyFile();
		trace("load " + dat);

		final FileInputStream o = new FileInputStream(dat);
		try {
			final Properties p = new Properties();
			p.load(o);

			mappings.clear();
			final Iterator keyItr = p.keySet().iterator();
			while (keyItr.hasNext()) {
				final String key = keyItr.next().toString();
				if (RepositoryMapping.isInitialKey(key)) {
					mappings.add(new RepositoryMapping(p, key));
				}
			}
		} finally {
			o.close();
		}

		remapAll();
		return this;
	}

	private void remapAll() {
		protectedResources.clear();
		final Iterator i = mappings.iterator();
		while (i.hasNext()) {
			map((RepositoryMapping) i.next());
		}
	}

	private void map(final RepositoryMapping m) {
		final IResource r;
		final File git;
		final IResource dotGit;
		IContainer c = null;

		m.clear();
		r = getProject().findMember(m.getContainerPath());
		if (r instanceof IContainer) {
			c = (IContainer) r;
		} else {
			c = (IContainer) r.getAdapter(IContainer.class);
		}

		if (c == null) {
			Activator.logError(CoreText.GitProjectData_mappedResourceGone,
					new FileNotFoundException(m.getContainerPath().toString()));
			m.clear();
			return;
		}
		m.setContainer(c);

		git = c.getLocation().append(m.getGitDirPath()).toFile();
		if (!git.isDirectory() || !new File(git, "config").isFile()) {
			Activator.logError(CoreText.GitProjectData_mappedResourceGone,
					new FileNotFoundException(m.getContainerPath().toString()));
			m.clear();
			return;
		}

		try {
			m.setRepository(lookupRepository(git));
		} catch (IOException ioe) {
			Activator.logError(CoreText.GitProjectData_mappedResourceGone,
					new FileNotFoundException(m.getContainerPath().toString()));
			m.clear();
			return;
		}

		m.fireRepositoryChanged();

		trace("map " + c + " -> " + m.getRepository());
		try {
			c.setSessionProperty(MAPPING_KEY, m);
		} catch (CoreException err) {
			Activator.logError("Failed to cache RepositoryMapping", err);
		}

		dotGit = c.findMember(".git");
		if (dotGit != null && dotGit.getLocation().toFile().equals(git)) {
			protect(dotGit);
		}
	}

	private void protect(IResource c) {
		while (c != null && !c.equals(getProject())) {
			trace("protect " + c);
			protectedResources.add(c);
			c = c.getParent();
		}
	}
}
