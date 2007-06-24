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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.GitIndex.Entry;

public class RepositoryMapping {
	public static boolean isInitialKey(final String key) {
		return key.endsWith(".gitdir");
	}

	private final String containerPath;

	private final String gitdirPath;

	private final String subset;

	private Repository db;

	private CheckpointJob currowj;

	private boolean runningowj;

	private IContainer container;

	public RepositoryMapping(final Properties p, final String initialKey) {
		final int dot = initialKey.lastIndexOf('.');
		String s;

		containerPath = initialKey.substring(0, dot);
		gitdirPath = p.getProperty(initialKey);
		s = p.getProperty(containerPath + ".subset");
		subset = "".equals(s) ? null : s;
	}

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

		p = "refs/eclipse/"
				+ container.getWorkspace().getRoot().getLocation()
						.lastSegment() + "/";
		IPath r = container.getFullPath();
		for (int j = 0; j < r.segmentCount(); j++) {
			if (j > 0)
				p += "-";
			p += r.segment(j);
		}
	}

	public IPath getContainerPath() {
		return Path.fromPortableString(containerPath);
	}

	public IPath getGitDirPath() {
		return Path.fromPortableString(gitdirPath);
	}

	public String getSubset() {
		return subset;
	}

	public File getWorkDir() {
//		assert containerPath.endsWith("/" + subset);
//		return Path.fromPortableString(
//				containerPath.substring(containerPath.length() - 1
//						- subset.length())).toFile();
		return getRepository().getDirectory().getParentFile();
	}

	public synchronized void clear() {
		db = null;
		currowj = null;
		container = null;
	}

	public synchronized Repository getRepository() {
		return db;
	}

	public synchronized void setRepository(final Repository r) {
		db = r;
		if (db != null) {
			initJob();
		}
	}

	public synchronized IContainer getContainer() {
		return container;
	}

	public synchronized void setContainer(final IContainer c) {
		container = c;
	}

	public synchronized void checkpointIfNecessary() {
		if (!runningowj) {
			currowj.scheduleIfNecessary();
		}
	}

	public synchronized void fullUpdate() {
		recomputeMerge();
		currowj.scheduleIfNecessary();
	}

	public synchronized void recomputeMerge() {
		GitProjectData.fireRepositoryChanged(this);
	}

	public synchronized Tree mapHEADTree() throws IOException,
			MissingObjectException {
		Tree head = getRepository().mapTree(Constants.HEAD);
		if (head != null) {
			if (getSubset() != null) {
				final TreeEntry e = head.findTreeMember(getSubset());
				e.detachParent();
				head = e instanceof Tree ? (Tree) e : null;
			}
		}
		if (head == null) {
			head = new Tree(getRepository());
		}
		return head;
	}

	public synchronized void store(final Properties p) {
		p.setProperty(containerPath + ".gitdir", gitdirPath);
		if (subset != null && !"".equals(subset)) {
			p.setProperty(containerPath + ".subset", subset);
		}
	}

	public String toString() {
		return "RepositoryMapping[" + containerPath + " -> " + gitdirPath + "]";
	}

	@SuppressWarnings("synthetic-access")
	private void initJob() {
		currowj = new CheckpointJob(this);
		currowj.addJobChangeListener(new JobChangeAdapter() {
			public void running(final IJobChangeEvent event) {
				synchronized (RepositoryMapping.this) {
					runningowj = true;
					initJob();
				}
			}

			public void done(final IJobChangeEvent event) {
				synchronized (RepositoryMapping.this) {
					runningowj = false;
				}
			}
		});
	}

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
//		System.out.println("HashesDiffer: " + rsrc);
		return hashesDiffer || entry.isModified(getWorkDir());
	}

	public String getRepoRelativePath(IResource rsrc) {
		String prefix = getSubset();
		String projectRelativePath = rsrc.getProjectRelativePath().toString();
		String repoRelativePath;
		if (prefix != null)
			repoRelativePath = prefix + "/" + projectRelativePath;
		else
			repoRelativePath = projectRelativePath;
		return repoRelativePath;
	}
}
