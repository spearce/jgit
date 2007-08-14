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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.ITag;
import org.eclipse.team.core.history.provider.FileRevision;
import org.spearce.egit.core.GitStorage;
import org.spearce.egit.core.GitTag;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.TopologicalSorter;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

public class GitFileRevision extends FileRevision {

	private final IResource resource;

	private final ObjectId commitId;

	private final int count;

	private TopologicalSorter<ObjectId>.Lane lane;

	public GitFileRevision(ObjectId commitId, IResource resource, int count) {
		this.count = count;
		this.commitId = commitId;
		this.resource = resource;
	}

	public String getName() {
		return resource.toString();
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		return new GitStorage(getCommit().getTreeId(), resource);
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(IProgressMonitor monitor)
			throws CoreException {
		return this;
	}

	public long getTimestamp() {
		PersonIdent author = getCommit().getAuthor();
		if (author != null)
			return author.getWhen().getTime();
		else
			return 0;
	}

	public String getContentIdentifier() {
		return commitId.toString();
	}

	public String getAuthor() {
		PersonIdent author = getCommit().getAuthor();
		if (author != null)
			return author.getName();
		else
			return null;
	}

	public String getComment() {
		return getCommit().getMessage();
	}

	public String toString() {
		char[] indent = new char[lane.getNumber()];
		Arrays.fill(indent,' ');
		String indents = new String(indent);
		Commit commit = getCommit();
		if (commit == null)
			return "WORKSPACE:" + resource.toString();
		else
			return indents+commit.toString() + ":" + resource.toString();
	}

	public URI getURI() {
		return resource.getLocationURI();
	}

	public ITag[] getTags() {
		Repository repository = RepositoryMapping.getMapping(resource).getRepository();
		Collection allTags = repository.getTags();
		Collection ret = new ArrayList();
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
		try {
			return RepositoryMapping.getMapping(resource).getRepository().mapCommit(commitId);
		} catch (IOException e) {
			throw new Error("Failed to get commit "+commitId, e);
		}
	}

	public ObjectId getCommitId() {
		return commitId;
	}

	public IResource getResource() {
		return resource;
	}

	public int getCount() {
		return count;
	}

	public TopologicalSorter<ObjectId>.Lane getLane() {
		return lane;
	}

	public void setLane(TopologicalSorter<ObjectId>.Lane lane) {
		this.lane = lane;
	}

	public TreeEntry getTreeEntry() {
		RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(resource);
		Tree tree;
		try {
			tree = repositoryMapping.getRepository().mapTree(getCommit().getTreeId());
			String path = repositoryMapping.getRepoRelativePath(resource);
			if (path.equals(""))
				return tree;
			if (resource.getType() == IResource.FILE)
				return tree.findBlobMember(path);
			else
				return tree.findBlobMember(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
