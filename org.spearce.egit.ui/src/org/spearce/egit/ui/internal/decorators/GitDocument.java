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
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.IndexChangedEvent;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.RefsChangedEvent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryListener;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

class GitDocument extends Document implements RepositoryListener {
	private final IResource resource;
	static Map<GitDocument,Repository> doc2repo = new WeakHashMap<GitDocument, Repository>();

	static GitDocument create(final IResource resource) throws IOException {
		Activator.trace("(GitDocument) create: " + resource);
		GitDocument ret = null;
		if (RepositoryProvider.getProvider(resource.getProject()) instanceof GitProvider) {
			ret = new GitDocument(resource);
			ret.populate();
		}
		return ret;
	}

	private GitDocument(IResource resource) {
		this.resource = resource;
		GitDocument.doc2repo.put(this, getRepository());
	}

	void populate() throws IOException {
		Activator.trace("(GitDocument) populate: " + resource);
		set("");
		final IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null)
			return;
		final String gitPath = mapping.getRepoRelativePath(resource);
		final Repository repository = getRepository();
		repository.addRepositoryChangedListener(this);
		String baseline = GitQuickDiffProvider.baseline.get(repository);
		if (baseline == null)
			baseline = Constants.HEAD;
		Tree baselineTree = repository.mapTree(baseline);
		if (baselineTree == null) {
			Activator.logError("Could not resolve quickdiff baseline "
					+ baseline + " corresponding to " + resource + " in "
					+ repository, new Throwable());
			return;
		}
		TreeEntry blobEnry = baselineTree.findBlobMember(gitPath);
		if (blobEnry != null) {
			Activator.trace("(GitDocument) compareTo: " + baseline);
			ObjectLoader loader = repository.openBlob(blobEnry.getId());
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
			set(s);
			Activator.trace("(GitDocument) has reference doc, size=" + s.length() + " bytes");
		} else {
			Activator.trace("(GitDocument) no revision.");
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
