/*
 *  Copyright (C) 2006  Robin Rosenberg
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
package org.spearce.egit.core.internal.mapping;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.ITag;
import org.eclipse.team.core.history.provider.FileRevision;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.GitStorage;
import org.spearce.egit.core.GitTag;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

public class GitFileRevision extends FileRevision {

	private final IResource resource;

	private final Commit commit;

	private final int count;

	public GitFileRevision(Commit commit, IResource resource, int count) {
		this.count = count;
		this.commit = commit;
		this.resource = resource;
	}

	public String getName() {
		return resource.toString();
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		return new GitStorage(commit.getTreeId(), resource);
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(IProgressMonitor monitor)
			throws CoreException {
		return this;
	}

	public long getTimestamp() {
		PersonIdent author = commit.getAuthor();
		if (author != null)
			return author.getWhen().getTime();
		else
			return 0;
	}

	public String getContentIdentifier() {
		return commit.getCommitId().toString();
	}

	public String getAuthor() {
		PersonIdent author = commit.getAuthor();
		if (author != null)
			return author.getName();
		else
			return null;
	}

	public String getComment() {
		return commit.getMessage();
	}

	public String toString() {
		if (commit == null)
			return "WORKSPACE:" + resource.toString();
		else
			return commit.toString() + ":" + resource.toString();
	}

	public URI getURI() {
		return resource.getLocationURI();
	}

	public ITag[] getTags() {
		GitProvider provider = (GitProvider) RepositoryProvider
				.getProvider(resource.getProject());
		Repository repository = provider.getData().getRepositoryMapping(
				resource.getProject()).getRepository();
		Collection allTags = repository.getTags();
		Collection ret = new ArrayList();
		ObjectId commitId = commit.getCommitId();
		for (Iterator i = allTags.iterator(); i.hasNext();) {
			String tag = (String) i.next();
			try {
				ObjectId id = repository.resolve(tag);
				if (id.equals(commitId))
					ret.add(new GitTag(tag));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return (ITag[]) ret.toArray(new ITag[ret.size()]);
	}

	public Commit getCommit() {
		return commit;
	}

	public IResource getResource() {
		return resource;
	}

	public int getCount() {
		return count;
	}

	public TreeEntry getTreeEntry() {
		GitProvider provider = (GitProvider) RepositoryProvider
				.getProvider(resource.getProject());
		RepositoryMapping repositoryMapping = provider.getData()
				.getRepositoryMapping(resource.getProject());
		Tree tree;
		try {
			tree = repositoryMapping.getRepository().mapTree(getCommit().getTreeId());
			String prefix = repositoryMapping.getSubset();
			if (prefix != null) {
				String relPath = resource.getProjectRelativePath().toString();
				if (relPath.equals(""))
					return tree;
				else {
					prefix = prefix + "/";
					String name = prefix + relPath;
					return tree.findMember(name);
				}
			} else
				return tree;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
