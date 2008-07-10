/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.decorators;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.jgit.lib.IndexChangedEvent;
import org.spearce.jgit.lib.RefsChangedEvent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryListener;

class GitDocument extends Document implements RepositoryListener {
	private final IResource resource;

	static GitDocument create(IResource resource) throws IOException, CoreException {
		GitDocument ret = null;
		if (RepositoryProvider.getProvider(resource.getProject()) instanceof GitProvider) {
			ret = new GitDocument(resource);
			ret.populate();
		}
		return ret;
	}

	private GitDocument(IResource resource) {
		this.resource = resource;
	}

	void populate() throws IOException, CoreException {
		set("");
		IProject project = resource.getProject();
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		getRepository().addRepositoryChangedListener(this);
		IFileHistoryProvider fileHistoryProvider = provider
				.getFileHistoryProvider();
		IFileHistory fileHistoryFor = fileHistoryProvider.getFileHistoryFor(
				resource, IFileHistoryProvider.SINGLE_REVISION, null);
		IFileRevision[] revisions = fileHistoryFor.getFileRevisions();
		if (revisions != null && revisions.length > 0) {
			IFileRevision revision = revisions[0];
			Activator.trace("(GitQuickDiffProvider) compareTo: "
					+ revision.getContentIdentifier());
			IStorage storage = revision.getStorage(null);
			InputStream contents = storage.getContents();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					contents));
			final int DEFAULT_FILE_SIZE = 15 * 1024;

			CharArrayWriter caw = new CharArrayWriter(DEFAULT_FILE_SIZE);
			char[] readBuffer = new char[2048];
			int n = in.read(readBuffer);
			while (n > 0) {
				caw.write(readBuffer, 0, n);
				n = in.read(readBuffer);
			}
			String s = caw.toString();
			set(s);
			Activator.trace("(GitQuickDiffProvider) has reference doc, size=" + s.length() + " bytes");
		} else {
			Activator.trace("(GitQuickDiffProvider) no revision.");
		}
	}

	void dispose() {
		getRepository().removeRepositoryChangedListener(this);
	}

	public void refsChanged(final RefsChangedEvent e) {
		try {
			populate();
		} catch (IOException e1) {
			Activator.logError("Failed to refresh quickdiff", e1);
		} catch (CoreException e1) {
			Activator.logError("Failed to refresh quickdiff", e1);
		}
	}

	public void indexChanged(final IndexChangedEvent e) {
		// Index not relevant at this moment
	}

	private Repository getRepository() {
		IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		return mapping.getRepository();
	}
}
