/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
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
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.RefLogWriter;
import org.spearce.jgit.lib.RefUpdate;
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
			if (type == ResetType.MIXED)
				resetIndex();
			else
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
			previousCommit = repository.mapCommit(repository.resolve(Constants.HEAD));
		} catch (IOException e) {
			throw new TeamException("looking up HEAD commit", e);
		}
	}

	private void writeRef() throws TeamException {
		try {
			final RefUpdate ru = repository.updateRef(Constants.HEAD);
			ru.setNewObjectId(commit.getCommitId());
			ru.setRefLogMessage("reset", false);
			if (ru.forceUpdate() == RefUpdate.Result.LOCK_FAILURE)
				throw new TeamException("Can't update " + ru.getName());
		} catch (IOException e) {
			throw new TeamException("Updating " + Constants.HEAD + " failed", e);
		}
	}

	private void readIndex() throws TeamException {
		try {
			newTree = commit.getTree();
			index = repository.getIndex();
		} catch (IOException e) {
			throw new TeamException("Reading index", e);
		}
	}

	private void resetIndex() throws TeamException {
		try {
			newTree = commit.getTree();
			index = repository.getIndex();
			index.readTree(newTree);
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
			writeReflog(Constants.HEAD);
			writeReflog(repository.getFullBranch());
		} catch (IOException e) {
			throw new TeamException("Writing reflogs", e);
		}
	}
}

