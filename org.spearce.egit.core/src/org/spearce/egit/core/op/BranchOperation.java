/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
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
import org.spearce.jgit.errors.CheckoutConflictException;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.RefLogWriter;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.WorkDirCheckout;

/**
 * This class implements checkouts of a specific revision. A check
 * is made that this can be done without data loss.
 */
public class BranchOperation implements IWorkspaceRunnable {

	private final Repository repository;

	private final String refName;

	/**
	 * Construct a {@link BranchOperation} object.
	 * @param repository
	 * @param refName Name of git ref to checkout
	 */
	public BranchOperation(Repository repository, String refName) {
		this.repository = repository;
		this.refName = refName;
	}

	private Tree oldTree;

	private GitIndex index;

	private Tree newTree;
	
	private Commit oldCommit;

	private Commit newCommit;



	public void run(IProgressMonitor monitor) throws CoreException {
		lookupRefs();
		monitor.worked(1);

		mapObjects();
		monitor.worked(1);

		checkoutTree();
		monitor.worked(1);

		writeIndex();
		monitor.worked(1);

		updateHeadRef();
		monitor.worked(1);

		writeHeadReflog();
		monitor.worked(1);

		refreshProjects();
		monitor.worked(1);

		monitor.done();
	}

	private void refreshProjects() {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
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

	private void writeHeadReflog() throws TeamException {
		try {
			RefLogWriter.writeReflog(repository, oldCommit.getCommitId(),
					newCommit.getCommitId(), "checkout: moving to " + refName,
					Constants.HEAD);
		} catch (IOException e) {
			throw new TeamException("Writing HEAD's reflog", e);
		}
	}

	private void updateHeadRef() throws TeamException {
		try {
			repository.writeSymref(Constants.HEAD, refName);
		} catch (IOException e) {
			throw new TeamException("Updating HEAD to ref: " + refName, e);
		}
	}

	private void writeIndex() throws TeamException {
		try {
			index.write();
		} catch (IOException e) {
			throw new TeamException("Writing index", e);
		}
	}

	private void checkoutTree() throws TeamException {
		try {
			new WorkDirCheckout(repository, repository.getWorkDir(), oldTree,
					index, newTree).checkout();
		} catch (CheckoutConflictException e) {
			TeamException teamException = new TeamException(e.getMessage());
			throw teamException;
		} catch (IOException e) {
			throw new TeamException("Problem while checking out:", e);
		}
	}

	private void mapObjects() throws TeamException {
		try {
			oldTree = oldCommit.getTree();
			index = repository.getIndex();
			newTree = newCommit.getTree();
		} catch (IOException e) {
			throw new TeamException("Mapping trees", e);
		}
	}

	private void lookupRefs() throws TeamException {
		try {
			newCommit = repository.mapCommit(refName);
		} catch (IOException e) {
			throw new TeamException("Mapping commit: " + refName, e);
		}

		try {
			oldCommit = repository.mapCommit(Constants.HEAD);
		} catch (IOException e) {
			throw new TeamException("Mapping commit HEAD commit", e);
		}
	}

}
