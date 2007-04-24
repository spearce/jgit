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
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.GitProvider;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

public class GitProjectData {
	private static final Map projectDataCache = new HashMap();

	private static final Map repositoryCache = new HashMap();

	private static RepositoryChangeListener[] repositoryChangeListeners = {};

	private static final IResourceChangeListener rcl = new RCL();

	private static class RCL implements IResourceChangeListener {
		public void resourceChanged(final IResourceChangeEvent event) {
			switch (event.getType()) {
			case IResourceChangeEvent.POST_CHANGE:
				projectsChanged(event.getDelta().getAffectedChildren(
						IResourceDelta.CHANGED));
				break;
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

	public static void attachToWorkspace(final boolean includeChange) {
		trace("attachToWorkspace - addResourceChangeListener");
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				rcl,
				(includeChange ? IResourceChangeEvent.POST_CHANGE : 0)
						| IResourceChangeEvent.PRE_CLOSE
						| IResourceChangeEvent.PRE_DELETE);
	}

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
		for (int k = repositoryChangeListeners.length - 1; k >= 0; k--) {
			if (repositoryChangeListeners[k] == objectThatCares)
				return;
		}
		final int p = repositoryChangeListeners.length;
		final RepositoryChangeListener[] n;
		n = new RepositoryChangeListener[p + 1];
		System.arraycopy(repositoryChangeListeners, 0, n, 0, p);
		n[p] = objectThatCares;
		repositoryChangeListeners = n;
	}

	/**
	 * Notify registered {@link RepositoryChangeListener}s of a change.
	 * 
	 * @param which
	 *            the repository which has had changes occur within it.
	 */
	static void fireRepositoryChanged(final RepositoryMapping which) {
		final RepositoryChangeListener[] e = getRepositoryChangeListeners();
		for (int k = e.length - 1; k >= 0; k--)
			e[k].repositoryChanged(which);
	}

	private static synchronized RepositoryChangeListener[] getRepositoryChangeListeners() {
		return repositoryChangeListeners;
	}

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

	public static synchronized void checkpointAllProjects() {
		final Iterator i = projectDataCache.values().iterator();
		while (i.hasNext()) {
			((GitProjectData) i.next()).checkpointIfNecessary();
		}
	}

	private static void trace(final String m) {
		Activator.trace("(GitProjectData) " + m);
	}

	private static void projectsChanged(final IResourceDelta[] projDeltas) {
		for (int k = 0; k < projDeltas.length; k++) {
			final IResource r = projDeltas[k].getResource();
			final GitProjectData d = get((IProject) r);
			if (d != null) {
				d.notifyChanged(projDeltas[k]);
			}
		}
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
		return (GitProjectData) projectDataCache.get(p);
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

		final Reference r = (Reference) repositoryCache.get(gitDir);
		Repository d = r != null ? (Repository) r.get() : null;
		if (d == null) {
			d = new Repository(gitDir);
			repositoryCache.put(gitDir, new WeakReference(d));
		}
		return d;
	}

	private final IProject project;

	private final Collection mappings;

	private final Map c2mapping;

	private final Set protectedResources;

	public GitProjectData(final IProject p) {
		project = p;
		mappings = new ArrayList();
		c2mapping = new HashMap();
		protectedResources = new HashSet();
	}

	public IProject getProject() {
		return project;
	}

	public void setRepositoryMappings(final Collection newMappings) {
		mappings.clear();
		mappings.addAll(newMappings);
		remapAll();
	}

	public void markTeamPrivateResources() throws CoreException {
		final Iterator i = c2mapping.entrySet().iterator();
		while (i.hasNext()) {
			final Map.Entry e = (Map.Entry) i.next();
			final IContainer c = (IContainer) e.getKey();
			final IResource dotGit = c.findMember(".git");
			if (dotGit != null) {
				try {
					final Repository r = ((RepositoryMapping) e.getValue())
							.getRepository();
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

	public boolean isProtected(final IResource f) {
		return protectedResources.contains(f);
	}

	public RepositoryMapping getRepositoryMapping(final IResource r) {
		return (RepositoryMapping) c2mapping.get(r);
	}

	public TreeEntry[] getActiveDiffTreeEntries(IResource res)
			throws CoreException {
		String s = null;
		RepositoryMapping m = null;

		IResource r = res;
		while (r != null) {
			m = getRepositoryMapping(r);
			if (m != null) {
				break;
			}

			if (s != null) {
				s = r.getName() + "/" + s;
			} else {
				s = r.getName();
			}

			r = r.getParent();
		}

		if (s != null && m != null && m.getActiveDiff() != null) {
			try {
				if (res.getType() == IResource.FILE)
					return m.getActiveDiff().findBlobMember(s);
				else
					return m.getActiveDiff().findTreeMember(s);
			} catch (IOException ioe) {
				throw Activator.error(
						CoreText.GitProjectData_lazyResolveFailed, ioe);
			}
		}

		return null;
	}

	public void checkpointIfNecessary() {
		final Iterator i = c2mapping.values().iterator();
		while (i.hasNext()) {
			((RepositoryMapping) i.next()).checkpointIfNecessary();
		}
	}

	public void fullUpdate() {
		final Iterator i = c2mapping.values().iterator();
		while (i.hasNext()) {
			try {
				((RepositoryMapping) i.next()).fullUpdate();
			} catch (IOException ioe) {
				Activator.logError(CoreText.GitProjectData_cannotReadHEAD, ioe);
				return;
			}
		}
	}

	public void delete() {
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

	private void notifyChanged(final IResourceDelta projDelta) {
		final Set affectedMappings = new HashSet();
		try {
			projDelta.accept(new IResourceDeltaVisitor() {
				public boolean visit(final IResourceDelta d)
						throws CoreException {
					final int f = d.getFlags();
					IResource r = d.getResource();
					if ((f & IResourceDelta.CONTENT) != 0
							|| (f & IResourceDelta.ENCODING) != 0
							|| r instanceof IContainer) {
						String s = null;
						RepositoryMapping m = null;

						while (r != null) {
							m = getRepositoryMapping(r);
							if (m != null) {
								break;
							}

							if (s != null) {
								s = r.getName() + "/" + s;
							} else {
								s = r.getName();
							}

							r = r.getParent();
						}

						if (m == null) {
							return false;
						} else if (s == null) {
							return true;
						}

						final Tree cacheTree = m.getCacheTree();
						if (cacheTree != null) {
							try {
								synchronized (cacheTree) {
									final TreeEntry e;
									if (r.getType() == IResource.FILE)
										e = cacheTree.findBlobMember(s);
									else
										e = cacheTree.findTreeMember(s);
									if (e instanceof FileTreeEntry) {
										trace("modified " + r + " -> "
												+ e.getFullName());
										e.setModified();
										affectedMappings.add(m);
									}
								}
							} catch (IOException ioe) {
								throw Activator
										.error(
												CoreText.GitProjectData_lazyResolveFailed,
												ioe);
							}
							return true;
						}
					}
					return false;
				}
			});
		} catch (CoreException ce) {
			// We are in deep trouble. This should NOT have happend. Detach
			// our listeners and forget it ever did.
			//
			attachToWorkspace(false);
			Activator.logError(CoreText.GitProjectData_notifyChangedFailed, ce);
		}

		try {
			final Iterator i = affectedMappings.iterator();
			while (i.hasNext()) {
				((RepositoryMapping) i.next()).recomputeMerge();
			}
		} catch (IOException ioe) {
			Activator
					.logError(CoreText.GitProjectData_notifyChangedFailed, ioe);
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

		try {
			m.recomputeMerge();
		} catch (IOException ioe) {
			Activator.logError(CoreText.GitProjectData_cannotReadHEAD, ioe);
			m.clear();
			return;
		}

		trace("map " + c + " -> " + m.getRepository());
		c2mapping.put(c, m);

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
