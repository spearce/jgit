/*******************************************************************************
 * Copyright (C) 2007, IBM Corporation and others
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/

package org.spearce.egit.ui.internal.decorators;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.team.core.Team;
import org.spearce.egit.core.AdaptableFileTreeIterator;
import org.spearce.egit.core.ContainerTreeIterator;
import org.spearce.egit.core.ContainerTreeIterator.ResourceEntry;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIPreferences;
import org.spearce.jgit.dircache.DirCache;
import org.spearce.jgit.dircache.DirCacheEntry;
import org.spearce.jgit.dircache.DirCacheIterator;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.treewalk.EmptyTreeIterator;
import org.spearce.jgit.treewalk.TreeWalk;
import org.spearce.jgit.treewalk.WorkingTreeIterator;
import org.spearce.jgit.treewalk.filter.AndTreeFilter;
import org.spearce.jgit.treewalk.filter.PathFilterGroup;
import org.spearce.jgit.treewalk.filter.TreeFilter;

class DecoratableResourceAdapter implements IDecoratableResource {

	private final IResource resource;

	private final RepositoryMapping mapping;

	private final Repository repository;

	private final ObjectId headId;

	private final IPreferenceStore store;

	private String branch = "";

	private boolean tracked = false;

	private boolean ignored = false;

	private boolean dirty = false;

	private boolean conflicts = false;

	private boolean assumeValid = false;

	private Staged staged = Staged.NOT_STAGED;

	static final int T_HEAD = 0;

	static final int T_INDEX = 1;

	static final int T_WORKSPACE = 2;

	@SuppressWarnings("fallthrough")
	public DecoratableResourceAdapter(IResource resourceToWrap)
			throws IOException {
		resource = resourceToWrap;
		mapping = RepositoryMapping.getMapping(resource);
		repository = mapping.getRepository();
		headId = repository.resolve(Constants.HEAD);

		store = Activator.getDefault().getPreferenceStore();

		// TODO: Add option to shorten branch name to 6 chars if it's a SHA
		branch = repository.getBranch();

		TreeWalk treeWalk = createThreeWayTreeWalk();
		if (treeWalk == null)
			return;

		switch (resource.getType()) {
		case IResource.FILE:
			if (!treeWalk.next())
				return;
			extractResourceProperties(treeWalk);
			break;
		case IResource.PROJECT:
			tracked = true;
		case IResource.FOLDER:
			extractContainerProperties(treeWalk);
			break;
		}
	}

	private void extractResourceProperties(TreeWalk treeWalk) {
		final ContainerTreeIterator workspaceIterator = treeWalk.getTree(
				T_WORKSPACE, ContainerTreeIterator.class);
		final ResourceEntry resourceEntry = workspaceIterator != null ? workspaceIterator
				.getResourceEntry() : null;

		if (resourceEntry == null)
			return;

		if (isIgnored(resourceEntry.getResource())) {
			ignored = true;
			return;
		}

		final int mHead = treeWalk.getRawMode(T_HEAD);
		final int mIndex = treeWalk.getRawMode(T_INDEX);

		if (mHead == FileMode.MISSING.getBits()
				&& mIndex == FileMode.MISSING.getBits())
			return;

		tracked = true;

		if (mHead == FileMode.MISSING.getBits()) {
			staged = Staged.ADDED;
		} else if (mIndex == FileMode.MISSING.getBits()) {
			staged = Staged.REMOVED;
		} else if (mHead != mIndex
				|| (mIndex != FileMode.TREE.getBits() && !treeWalk.idEqual(
						T_HEAD, T_INDEX))) {
			staged = Staged.MODIFIED;
		} else {
			staged = Staged.NOT_STAGED;
		}

		final DirCacheIterator indexIterator = treeWalk.getTree(T_INDEX,
				DirCacheIterator.class);
		final DirCacheEntry indexEntry = indexIterator != null ? indexIterator
				.getDirCacheEntry() : null;

		if (indexEntry == null)
			return;

		if (indexEntry.getStage() > 0)
			conflicts = true;

		if (indexEntry.isAssumeValid()) {
			dirty = false;
			assumeValid = true;
		} else {
			if (!timestampMatches(indexEntry, resourceEntry))
				dirty = true;

			// TODO: Consider doing a content check here, to rule out false
			// positives, as we might get mismatch between timestamps, even
			// if the content is the same.
		}
	}

	private class RecursiveStateFilter extends TreeFilter {

		private int filesChecked = 0;

		private int targetDepth = -1;

		private final int recurseLimit;

		public RecursiveStateFilter() {
			recurseLimit = store
					.getInt(UIPreferences.DECORATOR_RECURSIVE_LIMIT);
		}

		@Override
		public boolean include(TreeWalk treeWalk)
				throws MissingObjectException, IncorrectObjectTypeException,
				IOException {

			if (treeWalk.getFileMode(T_HEAD) == FileMode.MISSING
					&& treeWalk.getFileMode(T_INDEX) == FileMode.MISSING)
				return false;

			if (FileMode.TREE.equals(treeWalk.getRawMode(T_WORKSPACE)))
				return shouldRecurse(treeWalk);

			// Backup current state so far
			Staged wasStaged = staged;
			boolean wasDirty = dirty;
			boolean hadConflicts = conflicts;

			extractResourceProperties(treeWalk);
			filesChecked++;

			// Merge results with old state
			ignored = false;
			assumeValid = false;
			dirty = wasDirty || dirty;
			conflicts = hadConflicts || conflicts;
			if (staged != wasStaged && filesChecked > 1)
				staged = Staged.MODIFIED;

			return false;
		}

		private boolean shouldRecurse(TreeWalk treeWalk) {
			final WorkingTreeIterator workspaceIterator = treeWalk.getTree(
					T_WORKSPACE, WorkingTreeIterator.class);

			if (workspaceIterator instanceof AdaptableFileTreeIterator)
				return true;

			ResourceEntry resourceEntry = null;
			if (workspaceIterator != null)
				resourceEntry = ((ContainerTreeIterator) workspaceIterator)
						.getResourceEntry();

			if (resourceEntry == null)
				return true;

			IResource visitingResource = resourceEntry.getResource();
			if (targetDepth == -1) {
				if (visitingResource.equals(resource)
						|| visitingResource.getParent().equals(resource))
					targetDepth = treeWalk.getDepth();
				else
					return true;
			}

			if ((treeWalk.getDepth() - targetDepth) >= recurseLimit) {
				if (visitingResource.equals(resource))
					extractResourceProperties(treeWalk);

				return false;
			}

			return true;
		}

		@Override
		public TreeFilter clone() {
			RecursiveStateFilter clone = new RecursiveStateFilter();
			clone.filesChecked = this.filesChecked;
			return clone;
		}

		@Override
		public boolean shouldBeRecursive() {
			return true;
		}
	}

	private void extractContainerProperties(TreeWalk treeWalk) throws IOException {

		if (isIgnored(resource)) {
			ignored = true;
			return;
		}

		treeWalk.setFilter(AndTreeFilter.create(treeWalk.getFilter(),
				new RecursiveStateFilter()));
		treeWalk.setRecursive(true);

		treeWalk.next();
	}

	/**
	 * Adds a filter to the specified tree walk limiting the results to only
	 * those matching the resource specified by <code>resourceToFilterBy</code>
	 * <p>
	 * If the resource does not exists in the current repository, no filter is
	 * added and the method returns <code>false</code>. If the resource is a
	 * project, no filter is added, but the operation is considered a success.
	 *
	 * @param treeWalk
	 *            the tree walk to add the filter to
	 * @param resourceToFilterBy
	 *            the resource to filter by
	 *
	 * @return <code>true</code> if the filter could be added,
	 *         <code>false</code> otherwise
	 */
	private boolean addResourceFilter(final TreeWalk treeWalk,
			final IResource resourceToFilterBy) {
		Set<String> repositoryPaths = Collections.singleton(mapping
				.getRepoRelativePath(resourceToFilterBy));
		if (repositoryPaths.isEmpty())
			return false;

		if (repositoryPaths.contains(""))
			return true; // Project filter

		treeWalk.setFilter(PathFilterGroup.createFromStrings(repositoryPaths));
		return true;
	}

	/**
	 * Helper method to create a new tree walk between the repository, the
	 * index, and the working tree.
	 *
	 * @return the created tree walk, or null if it could not be created
	 * @throws IOException
	 *             if there were errors when creating the tree walk
	 */
	private TreeWalk createThreeWayTreeWalk() throws IOException {
		final TreeWalk treeWalk = new TreeWalk(repository);
		if (!addResourceFilter(treeWalk, resource))
			return null;

		treeWalk.setRecursive(treeWalk.getFilter().shouldBeRecursive());
		treeWalk.reset();

		// Repository
		if (headId != null)
			treeWalk.addTree(new RevWalk(repository).parseTree(headId));
		else
			treeWalk.addTree(new EmptyTreeIterator());

		// Index
		treeWalk.addTree(new DirCacheIterator(DirCache.read(repository)));

		// Working directory
		IProject project = resource.getProject();
		IWorkspaceRoot workspaceRoot = resource.getWorkspace().getRoot();
		File repoRoot = repository.getWorkDir();

		if (repoRoot.equals(project.getLocation().toFile()))
			treeWalk.addTree(new ContainerTreeIterator(project));
		else if (repoRoot.equals(workspaceRoot.getLocation().toFile()))
			treeWalk.addTree(new ContainerTreeIterator(workspaceRoot));
		else
			treeWalk.addTree(new AdaptableFileTreeIterator(repoRoot,
					workspaceRoot));

		return treeWalk;
	}

	private static boolean timestampMatches(DirCacheEntry indexEntry,
			ResourceEntry resourceEntry) {
		long tIndex = indexEntry.getLastModified();
		long tWorkspaceResource = resourceEntry.getLastModified();


		// C-Git under Windows stores timestamps with 1-seconds resolution,
		// so we need to check to see if this is the case here, and possibly
		// fix the timestamp of the resource to match the resolution of the
		// index.
		// It also appears the timestamp in Java on Linux may also be rounded
		// in which case the index timestamp may have subseconds, but not
		// the timestamp from the workspace resource.
		// If either timestamp looks rounded we skip the subscond part.
		if (tIndex % 1000 == 0 || tWorkspaceResource % 1000 == 0) {
			return tIndex / 1000 == tWorkspaceResource / 1000;
		} else {
			return tIndex == tWorkspaceResource;
		}
	}

	private static boolean isIgnored(IResource resource) {
		// TODO: Also read ignores from .git/info/excludes et al.
		return Team.isIgnoredHint(resource);
	}

	public String getName() {
		return resource.getName();
	}

	public int getType() {
		return resource.getType();
	}

	public String getBranch() {
		return branch;
	}

	public boolean isTracked() {
		return tracked;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public boolean isDirty() {
		return dirty;
	}

	public Staged staged() {
		return staged;
	}

	public boolean hasConflicts() {
		return conflicts;
	}

	public boolean isAssumeValid() {
		return assumeValid;
	}
}