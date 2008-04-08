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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.GitProvider;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.GitIndex.Entry;

/**
 * This class keeps track
 */
public class RepositoryMapping {
	static boolean isInitialKey(final String key) {
		return key.endsWith(".gitdir");
	}

	private final String containerPath;

	private final String gitdirPath;

	private final String subset;

	private Repository db;

	private IContainer container;

	/**
	 * Construct a {@link RepositoryMapping} for a previously connected project.
	 *
	 * @param p TODO
	 * @param initialKey TODO
	 */
	public RepositoryMapping(final Properties p, final String initialKey) {
		final int dot = initialKey.lastIndexOf('.');
		String s;

		containerPath = initialKey.substring(0, dot);
		gitdirPath = p.getProperty(initialKey);
		s = p.getProperty(containerPath + ".subset");
		subset = "".equals(s) ? null : s;
	}

	/**
	 * Construct a {@link RepositoryMapping} for previously
	 * unknown project.
	 *
	 * @param mappedContainer
	 * @param gitDir
	 * @param subsetRoot
	 */
	public RepositoryMapping(final IContainer mappedContainer,
			final File gitDir, final String subsetRoot) {
		final IPath cLoc = mappedContainer.getLocation()
				.removeTrailingSeparator();
		final IPath gLoc = Path.fromOSString(gitDir.getAbsolutePath())
				.removeTrailingSeparator();
		final IPath gLocParent = gLoc.removeLastSegments(1);
		String p;
		int cnt;

		container = mappedContainer;
		containerPath = container.getProjectRelativePath().toPortableString();

		if (cLoc.isPrefixOf(gLoc)) {
			gitdirPath = gLoc.removeFirstSegments(
					gLoc.matchingFirstSegments(cLoc)).toPortableString();
		} else if (gLocParent.isPrefixOf(cLoc)) {
			cnt = cLoc.segmentCount() - cLoc.matchingFirstSegments(gLocParent);
			p = "";
			while (cnt-- > 0) {
				p += "../";
			}
			p += gLoc.segment(gLoc.segmentCount() - 1);
			gitdirPath = p;
		} else {
			gitdirPath = gLoc.toPortableString();
		}

		subset = "".equals(subsetRoot) ? null : subsetRoot;
	}

	IPath getContainerPath() {
		return Path.fromPortableString(containerPath);
	}

	IPath getGitDirPath() {
		return Path.fromPortableString(gitdirPath);
	}

	/**
	 * Eclipse projects typically reside one or more levels
	 * below the repository. This method return the relative
	 * path to the project. Null is returned instead of "".
	 *
	 * @return relative path from repository to project, or null
	 */
	public String getSubset() {
		return subset;
	}

	/**
	 * @return the workdir file, i.e. where the files are checked out
	 */
	public File getWorkDir() {
		return getRepository().getWorkDir();
	}

	synchronized void clear() {
		db = null;
		container = null;
	}

	/**
	 * @return a reference to the repository object handled by this mapping
	 */
	public synchronized Repository getRepository() {
		return db;
	}

	synchronized void setRepository(final Repository r) {
		db = r;
	}

	/**
	 * @return the mapped container (currently project)
	 */
	public synchronized IContainer getContainer() {
		return container;
	}

	synchronized void setContainer(final IContainer c) {
		container = c;
	}

	/**
	 * Notify registered {@link RepositoryChangeListener}s of a change.
	 * 
	 * @see GitProjectData#addRepositoryChangeListener(RepositoryChangeListener)
	 */
	public void fireRepositoryChanged() {
		GitProjectData.fireRepositoryChanged(this);
	}

	synchronized void store(final Properties p) {
		p.setProperty(containerPath + ".gitdir", gitdirPath);
		if (subset != null && !"".equals(subset)) {
			p.setProperty(containerPath + ".subset", subset);
		}
	}

	public String toString() {
		return "RepositoryMapping[" + containerPath + " -> " + gitdirPath + "]";
	}

	/**
	 * Check whether a resource has been changed relative to the checked out
	 * version. Content is assumed changed by this routine if the resource's
	 * modification time differs from what is recorded in the index, but the
	 * real content hasn't changed. The reason is performance.
	 *
	 * @param rsrc
	 * @return true if a resource differs in the workdir or index relative to
	 *         HEAD
	 *
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public boolean isResourceChanged(IResource rsrc) throws IOException, UnsupportedEncodingException {
		Repository repository = getRepository();
		GitIndex index = repository.getIndex();
		String repoRelativePath = getRepoRelativePath(rsrc);
		Tree headTree = repository.mapTree("HEAD");
		TreeEntry blob = headTree!=null ? headTree.findBlobMember(repoRelativePath) : null;
		Entry entry = index.getEntry(repoRelativePath);
		if (rsrc instanceof IFile && entry == null && blob == null)
			return false;
		if (entry == null)
			return true; // flags new resources as changes
		if (blob == null)
			return true; // added in index
		boolean hashesDiffer = !entry.getObjectId().equals(blob.getId());
		return hashesDiffer || entry.isModified(getWorkDir());
	}

	/**
	 * @param rsrc
	 * @return the path relative to the Git repository, including base name.
	 */
	public String getRepoRelativePath(IResource rsrc) {
		String prefix = getSubset();
		String projectRelativePath = rsrc.getProjectRelativePath().toString();
		String repoRelativePath;
		if (prefix != null) {
			if (projectRelativePath.length() == 0)
				repoRelativePath = prefix;
			else
				repoRelativePath = prefix + "/" + projectRelativePath;
		} else
			repoRelativePath = projectRelativePath;

		assert repoRelativePath != null;
		return repoRelativePath;
	}

	/**
	 * Get the repository mapping for a resource
	 *
	 * @param resource
	 * @return the RepositoryMapping for this resource,
	 *         or null for non GitProvider.
	 */
	public static RepositoryMapping getMapping(IResource resource) {
		IProject project = resource.getProject();
		if (project == null)
			return null;
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (!(provider instanceof GitProvider))
			return null;
		GitProvider gp = (GitProvider)provider;
		RepositoryMapping repositoryMapping = gp.getData().getRepositoryMapping(project);
		return repositoryMapping;
	}
}
