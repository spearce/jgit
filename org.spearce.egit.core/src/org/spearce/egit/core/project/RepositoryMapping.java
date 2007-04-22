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
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.MergedTree;
import org.spearce.jgit.lib.RefLock;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

public class RepositoryMapping {
	public static boolean isInitialKey(final String key) {
		return key.endsWith(".gitdir");
	}

	private final String containerPath;

	private final String gitdirPath;

	private final String subset;

	private final String cacheref;

	private Repository db;

	private CheckpointJob currowj;

	private boolean runningowj;

	private IContainer container;

	private Tree cacheTree;

	private MergedTree activeDiff;

	public RepositoryMapping(final Properties p, final String initialKey) {
		final int dot = initialKey.lastIndexOf('.');
		String s;

		containerPath = initialKey.substring(0, dot);
		gitdirPath = p.getProperty(initialKey);
		s = p.getProperty(containerPath + ".subset");
		subset = "".equals(s) ? null : s;
		cacheref = p.getProperty(containerPath + ".cacheref");
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
		cacheref = p;
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

	public synchronized void clear() {
		db = null;
		currowj = null;
		container = null;
		cacheTree = null;
		activeDiff = null;
	}

	public synchronized Repository getRepository() {
		return db;
	}

	public synchronized void setRepository(final Repository r) {
		db = r;
		cacheTree = null;
		activeDiff = null;
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

	public synchronized Tree getCacheTree() {
		return cacheTree;
	}

	public synchronized MergedTree getActiveDiff() {
		return activeDiff;
	}

	public synchronized void checkpointIfNecessary() {
		if (!runningowj) {
			currowj.scheduleIfNecessary();
		}
	}

	public synchronized void saveCache() throws IOException {
		final RefLock lock = getRepository().lockRef(cacheref);
		if (lock != null) {
			lock.write(cacheTree.getId());
			lock.commit();
		}
	}

	public synchronized void fullUpdate() throws IOException {
		cacheTree = mapHEADTree();

		if (container.exists()) {
			cacheTree.accept(new UpdateTreeFromWorkspace(container),
					TreeEntry.CONCURRENT_MODIFICATION);
		} else {
			cacheTree.delete();
		}

		recomputeMerge();
		currowj.scheduleIfNecessary();
	}

	public synchronized void recomputeMerge() throws IOException {
		Tree head = mapHEADTree();

		if (cacheTree == null) {
			cacheTree = getRepository().mapTree(cacheref);
		}
		if (cacheTree == null) {
			cacheTree = new Tree(getRepository());
		}

		cacheTree.accept(new EnqueueWriteTree(container, currowj),
				TreeEntry.MODIFIED_ONLY);

		activeDiff = new MergedTree(new Tree[] { head, cacheTree });
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
		p.setProperty(containerPath + ".cacheref", cacheref);
		if (subset != null && !"".equals(subset)) {
			p.setProperty(containerPath + ".subset", subset);
		}
	}

	public String toString() {
		return "RepositoryMapping[" + containerPath + " -> " + gitdirPath
				+ ", " + cacheref + "]";
	}

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
}
