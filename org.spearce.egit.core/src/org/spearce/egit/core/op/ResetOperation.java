/*
 *  Copyright (C) 2007 Dave Watson <dwatson@mimvista.com>
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
package org.spearce.egit.core.op;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.RefLock;
import org.spearce.jgit.lib.RefLogWriter;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tag;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.WorkDirCheckout;

/**
 * A class for changing a ref and possibly index and workdir too.
 */
public class ResetOperation implements IWorkspaceRunnable {
	/**
	 * Kind of reset
	 */
	public enum ResetType {
		/**
		 * Just change the ref. The index and workdir are not changed.
		 */
		SOFT,

		/**
		 * Change the ref and the index. The workdir is not changed.
		 */
		MIXED,

		/**
		 * Change the ref, the index and the workdir
		 */
		HARD
	}
	
	private final Repository repository;
	private final String refName;
	private final ResetType type;
	
	private Commit commit;
	private Commit previousCommit;
	private Tree newTree;
	private GitIndex index;

	/**
	 * Construct a {@link ResetOperation}
	 *
	 * @param repository
	 * @param refName
	 * @param type
	 */
	public ResetOperation(Repository repository, String refName, ResetType type) {
		this.repository = repository;
		this.refName = refName;
		this.type = type;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Performing " + type.toString().toLowerCase() + " reset to " + refName, 7);
		
		mapObjects();
		monitor.worked(1);
		
		writeRef();
		monitor.worked(1);
		
		if (type != ResetType.SOFT) {
			readIndex();
			writeIndex();
		}
		monitor.worked(1);
		
		if (type == ResetType.HARD) {
			checkoutIndex();
		}
		monitor.worked(1);
		
		if (type != ResetType.SOFT) {
			refreshIndex();
		}
		monitor.worked(1);
		
		writeReflogs();
		monitor.worked(1);

		refreshProjects();
		
		monitor.done();
	}

	private void refreshIndex() throws TeamException {
//		File workdir = repository.getDirectory().getParentFile();
//		for (Entry e : newIndex.getMembers()) {
//			try {
//				e.update(new File(workdir, e.getName()));
//			} catch (IOException ignore) {}
//		}
		try {
			index.write();
		} catch (IOException e1) {
			throw new TeamException("Writing index", e1);
		}
	}

	private void refreshProjects() {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		final File parentFile = repository.getWorkDir();
		for (IProject p : projects) {
			final File file = p.getLocation().toFile();
			if (file.getAbsolutePath().startsWith(parentFile.getAbsolutePath())) {
				try {
					System.out.println("Refreshing " + p);
					p.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void mapObjects() throws TeamException {
		final ObjectId commitId;
		try {
			commitId = repository.resolve(refName);
		} catch (IOException e) {
			throw new TeamException("looking up ref " + refName, e);
		}
		try {
			commit = repository.mapCommit(commitId);
		} catch (IOException e) {
			try {
				Tag t = repository.mapTag(refName, commitId);
				commit = repository.mapCommit(t.getObjId());
			} catch (IOException e2) {
				throw new TeamException("looking up commit " + commitId, e2);
			}
		}
		
		try {
			previousCommit = repository.mapCommit(repository.resolve("HEAD"));
		} catch (IOException e) {
			throw new TeamException("looking up HEAD commit", e);
		}
	}

	private void writeRef() throws TeamException {
		RefLock lockRef;
		try {
			lockRef = repository.lockRef("HEAD");
		} catch (IOException e) {
			throw new TeamException("Could not lock ref for HEAD", e);
		}
		try {
			lockRef.write(commit.getCommitId());
		} catch (IOException e) {
			throw new TeamException("Could not write ref for HEAD", e);
		}
		
		if (!lockRef.commit()) 
			throw new TeamException("writing ref failed");
	}

	private void readIndex() throws TeamException {
		try {
			newTree = commit.getTree();
			index = repository.getIndex();
		} catch (IOException e) {
			throw new TeamException("Reading index", e);
		}
	}

	private void writeIndex() throws CoreException {
		try {
			index.write();
		} catch (IOException e) {
			throw new TeamException("Writing index", e);
		}
	}

	private void checkoutIndex() throws TeamException {
		final File parentFile = repository.getWorkDir();
		try {
			WorkDirCheckout workDirCheckout = 
				new WorkDirCheckout(repository, parentFile, index, newTree);
			workDirCheckout.setFailOnConflict(false);
			workDirCheckout.checkout();
		} catch (IOException e) {
			throw new TeamException("mapping tree for commit", e);
		}
	}


	private void writeReflog(String reflogRelPath) throws IOException {
		String name = refName;
		if (name.startsWith("refs/heads/"))
			name = name.substring(11);
		if (name.startsWith("refs/remotes/"))
			name = name.substring(13);
		
		String message = "reset --" + type.toString().toLowerCase() + " " + name;
		
		RefLogWriter.writeReflog(repository, previousCommit.getCommitId(), commit.getCommitId(), message, reflogRelPath);
	}

	private void writeReflogs() throws TeamException {
		try {
			writeReflog("HEAD");
			writeReflog(repository.getFullBranch());
		} catch (IOException e) {
			throw new TeamException("Writing reflogs", e);
		}
	}
}

