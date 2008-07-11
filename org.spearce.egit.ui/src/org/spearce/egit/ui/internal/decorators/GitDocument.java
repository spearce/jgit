/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.decorators;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.Document;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.jgit.lib.IndexChangedEvent;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.RefsChangedEvent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryListener;
import org.spearce.jgit.lib.TreeEntry;

class GitDocument extends Document implements RepositoryListener {
	private final IResource resource;

	static GitDocument create(final IResource resource) throws IOException {
		GitDocument ret = null;
		if (RepositoryProvider.getProvider(resource.getProject()) instanceof GitProvider) {
			ret = new GitDocument(resource);
			ret.populate();
		}
		return ret;
	}

	private GitDocument(IResource resource) {
		this.resource = resource;
		GitQuickDiffProvider.doc2repo.put(this, getRepository());
	}

	void populate() throws IOException {
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
			baseline = "HEAD";
		TreeEntry blobEnry = repository.mapTree(baseline).findBlobMember(gitPath);
		if (blobEnry != null) {
			Activator.trace("(GitQuickDiffProvider) compareTo: " + baseline);
			ObjectLoader loader = repository.openBlob(blobEnry.getId());
			byte[] bytes = loader.getBytes();
			String s = new String(bytes); // FIXME Platform default charset. should be Eclipse default
			set(s);
			Activator.trace("(GitQuickDiffProvider) has reference doc, size=" + s.length() + " bytes");
		} else {
			Activator.trace("(GitQuickDiffProvider) no revision.");
		}
	}

	void dispose() {
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
}
