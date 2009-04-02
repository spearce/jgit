/*******************************************************************************
 * Copyright (C) 2008, 2009 Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.IndexChangedEvent;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.RefsChangedEvent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryListener;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

class GitDocument extends Document implements RepositoryListener {
	private final IResource resource;

	private ObjectId lastCommit;
	private ObjectId lastTree;
	private ObjectId lastBlob;

	static Map<GitDocument,Repository> doc2repo = new WeakHashMap<GitDocument, Repository>();

	static GitDocument create(final IResource resource) throws IOException {
		Activator.trace("(GitDocument) create: " + resource);
		GitDocument ret = null;
		if (RepositoryProvider.getProvider(resource.getProject()) instanceof GitProvider) {
			ret = new GitDocument(resource);
			ret.populate();
			final Repository repository = ret.getRepository();
			if (repository != null)
				repository.addRepositoryChangedListener(ret);
		}
		return ret;
	}

	private GitDocument(IResource resource) {
		this.resource = resource;
		GitDocument.doc2repo.put(this, getRepository());
	}

	private void setResolved(final AnyObjectId commit, final AnyObjectId tree, final AnyObjectId blob, final String value) {
		lastCommit = commit != null ? commit.copy() : null;
		lastTree = tree != null ? tree.copy() : null;
		lastBlob = blob != null ? blob.copy() : null;
		set(value);
		if (blob != null)
			Activator.trace("(GitDocument) resolved " + resource + " to " + lastBlob + " in " + lastCommit + "/" + lastTree);
		else
			Activator.trace("(GitDocument) unresolved " + resource);
	}

	void populate() throws IOException {
		Activator.trace("(GitDocument) populate: " + resource);
		final IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null) {
			setResolved(null, null, null, "");
			return;
		}
		final String gitPath = mapping.getRepoRelativePath(resource);
		final Repository repository = mapping.getRepository();
		String baseline = GitQuickDiffProvider.baseline.get(repository);
		if (baseline == null)
			baseline = Constants.HEAD;
		ObjectId commitId = repository.resolve(baseline);
		if (commitId != null) {
			if (commitId.equals(lastCommit)) {
				Activator.trace("(GitDocument) already resolved");
				return;
			}
		} else {
			Activator.logError("Could not resolve quickdiff baseline "
					+ baseline + " corresponding to " + resource + " in "
					+ repository, new Throwable());
			setResolved(null, null, null, "");
			return;
		}
		Commit baselineCommit = repository.mapCommit(commitId);
		if (baselineCommit == null) {
			Activator.logError("Could not load commit " + commitId + " for "
					+ baseline + " corresponding to " + resource + " in "
					+ repository, new Throwable());
			setResolved(null, null, null, "");
			return;
		}
		ObjectId treeId = baselineCommit.getTreeId();
		if (treeId.equals(lastTree)) {
			Activator.trace("(GitDocument) already resolved");
			return;
		}
		Tree baselineTree = baselineCommit.getTree();
		if (baselineTree == null) {
			Activator.logError("Could not load tree " + treeId + " for "
					+ baseline + " corresponding to " + resource + " in "
					+ repository, new Throwable());
			setResolved(null, null, null, "");
			return;
		}
		TreeEntry blobEntry = baselineTree.findBlobMember(gitPath);
		if (blobEntry != null && !blobEntry.getId().equals(lastBlob)) {
			Activator.trace("(GitDocument) compareTo: " + baseline);
			ObjectLoader loader = repository.openBlob(blobEntry.getId());
			byte[] bytes = loader.getBytes();
			String charset;
			// Get the encoding for the current version. As a matter of
			// principle one might want to use the eclipse settings for the
			// version we are retrieving as that may be defined by the
			// project settings, but there is no historic API for this.
			IEncodedStorage encodedStorage = ((IEncodedStorage)resource);
			try {
				if (encodedStorage != null)
					charset = encodedStorage.getCharset();
				else
					charset = resource.getParent().getDefaultCharset();
			} catch (CoreException e) {
				charset = Constants.CHARACTER_ENCODING;
			}
			// Finally we could consider validating the content with respect
			// to the content. We don't do that here.
			String s = new String(bytes, charset);
			setResolved(commitId, baselineTree.getId(), blobEntry.getId(), s);
			Activator.trace("(GitDocument) has reference doc, size=" + s.length() + " bytes");
		} else {
			if (blobEntry == null)
				setResolved(null, null, null, "");
			else
				Activator.trace("(GitDocument) already resolved");
		}
	}

	void dispose() {
		Activator.trace("(GitDocument) dispose: " + resource);
		doc2repo.remove(this);
		Repository repository = getRepository();
		if (repository != null)
			repository.removeRepositoryChangedListener(this);
	}

	public void refsChanged(final RefsChangedEvent e) {
		try {
			populate();
		} catch (IOException e1) {
			Activator.logError("Failed to refresh quickdiff", e1);
		}
	}

	public void indexChanged(final IndexChangedEvent e) {
		// Index not relevant at this moment
	}

	private Repository getRepository() {
		IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping != null)
			return mapping.getRepository();
		return null;
	}

	/**
	 * A change occurred to a repository. Update any GitDocument instances
	 * referring to such repositories.
	 *
	 * @param repository Repository which changed
	 * @throws IOException
	 */
	static void refreshRelevant(final Repository repository) throws IOException {
		for (Map.Entry<GitDocument, Repository> i : doc2repo.entrySet()) {
			if (i.getValue() == repository) {
				i.getKey().populate();
			}
		}
	}
}
