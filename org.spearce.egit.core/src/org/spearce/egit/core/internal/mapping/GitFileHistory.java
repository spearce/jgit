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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.GitWorkspaceFileRevision;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.Walker;

public class GitFileHistory extends FileHistory implements IAdaptable {

	private final IResource resource;

	private final String[] relativeResourceName;

	private final int flags;

	private IFileRevision[] revisions;

	public GitFileHistory(IResource resource, int flags) {
		this.resource = resource;
		this.flags = flags;
		String prefix = getRepositoryMapping().getSubset();
		String[] prefixSegments = prefix!=null ? prefix.split("/") : new String[0];
		String[] resourceSegments = resource.getProjectRelativePath().segments(); 
		relativeResourceName = new String[prefixSegments.length + resourceSegments.length];
		System.arraycopy(prefixSegments, 0, relativeResourceName, 0, prefixSegments.length);
		System.arraycopy(resourceSegments, 0, relativeResourceName, prefixSegments.length, resourceSegments.length);
	}

	public IFileRevision[] getContributors(IFileRevision revision) {
		GitFileRevision grevision = (GitFileRevision) revision;
		ObjectId[] parents = grevision.getCommit().getParentIds();
		IFileRevision[] ret = new IFileRevision[parents.length];
		Repository repository = getRepository();
		for (int i = 0; i < parents.length; ++i) {
			try {
				ret[i] = new GitFileRevision(repository
						.mapCommit(parents[i]), grevision
				.getResource(), -1);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return ret;
	}

	public IFileRevision getFileRevision(String id) {
		if (id.equals("")) {
			return new GitWorkspaceFileRevision(resource);
		} else {
			try {
				Repository repository = getRepository();
				return new GitFileRevision(repository.mapCommit(id), resource, 0);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	private Repository getRepository() {
		return getRepositoryMapping().getRepository();
	}

	private GitProjectData getData() {
		GitProvider provider = (GitProvider) RepositoryProvider
				.getProvider(resource.getProject());
		return provider.getData();
	}

	private RepositoryMapping getRepositoryMapping() {
		return getData().getRepositoryMapping(resource.getProject());
	}

static class EclipseWalker extends Walker {
	IResource resource;
	
	EclipseWalker(Repository repository, Commit start, String[] relativeResourceName,boolean leafIsBlob,IResource resource,boolean followMainOnly, ObjectId lastActiveDiffId) {
		super(repository, start, relativeResourceName, leafIsBlob, followMainOnly, lastActiveDiffId);
		this.resource = resource;
	}

	protected void collect(Collection ret,Commit commit, int count) {
		ret.add(new GitFileRevision(commit, resource, count));		
	}
	
};

	public IFileRevision[] getFileRevisions() {
		if (revisions == null)
			if ((flags & IFileHistoryProvider.SINGLE_LINE_OF_DESCENT) == 0)
				findSingleRevision();
			else
				try {
					findRevisions();
				} catch (IOException e) {
					throw new Error(e);
				}
		return revisions;
	}

	/**
	 * Get a single file revision suitable for quickdiff.
	 * 
	 * We have two modes. For a branch set up for Stacked Git that has a patch
	 * return the revision prior to the topmost patch, be it another patch or a
	 * normal Git Commit. This is the revision in HEAD^. Otherwise we return the
	 * revision in HEAD.
	 */
	private void findSingleRevision() {
		try {
			Repository repository = getRepository();
			ObjectId id = repository.resolve("HEAD");
			Commit current = repository.mapCommit(id);
			if (repository.isStGitMode()) {
				ObjectId[] parentIds = current.getParentIds();
				if (parentIds != null && parentIds.length > 0)
					current = repository.mapCommit(parentIds[0]);
				else {
					revisions = new IFileRevision[0];
					return;
				}
			}
			TreeEntry currentEntry = current.getTree();
			for (int i=0; i < relativeResourceName.length && currentEntry != null; ++i) {
				if (i == relativeResourceName.length-1 && resource.getType() == IResource.FILE)
					((Tree)currentEntry).findBlobMember(relativeResourceName[i]);
				else
					((Tree)currentEntry).findTreeMember(relativeResourceName[i]);
			}
			if (currentEntry != null)
				revisions = new IFileRevision[] { new GitFileRevision(current, resource,
						-1) };
			else
				revisions = new IFileRevision[0];

		} catch (IOException e) {
			e.printStackTrace();
			revisions = new IFileRevision[0];
		}
	}

	private void findRevisions() throws IOException {
		RepositoryProvider provider = RepositoryProvider.getProvider(resource
				.getProject());
		if (provider instanceof GitProvider) {
			GitWorkspaceFileRevision wsrevision = new GitWorkspaceFileRevision(resource);

			long time0 = new Date().getTime();
			System.out.println("getting file history");
			List ret = new ArrayList();
			TreeEntry[] activeDiffTreeEntries = null;
			try {
				activeDiffTreeEntries = getData().getActiveDiffTreeEntries(resource);
			} catch (CoreException e1) {
				// TODO: eclipse excetion logging
				e1.printStackTrace();
			}
			ObjectId activeDiffLeafId = null;
			if (activeDiffTreeEntries!=null)
				activeDiffLeafId = activeDiffTreeEntries[0].getId();

			ObjectId head = getRepository().resolve("HEAD");
			Commit start = getRepository().mapCommit(head);
			EclipseWalker walker = new EclipseWalker(getRepository(), start, relativeResourceName, 
					resource.getType() == IResource.FILE, 
					resource, 
					(flags & IFileHistoryProvider.SINGLE_LINE_OF_DESCENT) == 0,
					activeDiffLeafId);
			Collection githistory = walker.collectHistory();
			if (githistory.size() >0) {
				if (resource.getType()==IResource.FILE) {
					// TODO: consider index in future versions
					try {
						InputStream wsContents = new BufferedInputStream(wsrevision.getStorage(null).getContents());
						InputStream headContents = ((IFileRevision)githistory.toArray()[0]).getStorage(null).getContents();
						if (!streamsEqual(wsContents,headContents)) {
							ret.add(wsrevision);
							ret.addAll(githistory);
						} else {
							ret.addAll(githistory);
						}
						wsContents.close();
						headContents.close();
					} catch (IOException e) {
						// TODO: Eclipse error handling
						e.printStackTrace();
					} catch (CoreException e) {
						// TODO: Eclipse error handling
						e.printStackTrace();
					}
				} else {
					ret.addAll(githistory);
				}
			} else {
				ret.add(wsrevision);
			}
			long time1 = new Date().getTime();
			System.out.println("got file history in " + (time1 - time0)
					/ 1000.0 + "s");

			revisions = (IFileRevision[]) ret.toArray(new IFileRevision[ret
					.size()]);

		} else {
			revisions = new IFileRevision[0];
		}
	}

	private boolean streamsEqual(InputStream s1, InputStream s2) {
		// Speed up...
		try {
			int c1,c2;
			while ((c1=s1.read()) == (c2=s2.read()) && c1!=-1)
				;
			return c1 == -1 && c2==-1;
		} catch (IOException e) {
			// TODO: eclipse error handling
			e.printStackTrace();
			return false;
		}
	}

	public IFileRevision[] getTargets(IFileRevision revision) {
		GitFileRevision grevision = (GitFileRevision) revision;
		ObjectId targetCommitId = grevision.getCommit().getCommitId();
		List ret = new ArrayList(4);
		for (int i = 0; i < revisions.length; ++i) {
			Commit ref = ((GitFileRevision) revisions[i]).getCommit();
			ObjectId[] parentIds = ref.getParentIds();
			for (int j = 0; j < parentIds.length; ++j) {
				if (parentIds[j].equals(targetCommitId)) {
					ret.add(revisions[i]);
					break;
				}
			}
		}
		return (IFileRevision[]) ret.toArray(new IFileRevision[ret.size()]);
	}

	public Object getAdapter(Class adapter) {
		System.out.println("GitFileHistory.getAdapter "+adapter.getName());
		return null;
	}

}
