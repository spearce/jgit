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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import org.spearce.egit.core.GitIndexFileRevision;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.GitWorkspaceFileRevision;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.MappedList;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.SuperList;
import org.spearce.jgit.lib.TopologicalWalker;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.GitIndex.Entry;

/**
 * A list of revisions for a specific resource according to some
 * filtering criterion. Though git really does not do file tracking,
 * this corresponds to listing all files with the same path.
 */
public class GitFileHistory extends FileHistory implements IAdaptable {

	private final IResource resource;

	private final String[] relativeResourceName;

	private final int flags;

	private List<IFileRevision> revisions;

	private final boolean returnAll;

	/**
	 * Construct a {@link GitFileHistory} object for a given resource (path)
	 * with some filtering applied. The filter could reduce the number of
	 * commits returned or just mark some versions as interesting.
	 *
	 * @param resource
	 * @param flags
	 *            See {@link IFileHistoryProvider}
	 * @param monitor
	 *            progress reporting facility
	 * @param returnAll
	 *            true if all versions should be collected even if the filter
	 *            does not match.
	 */
	public GitFileHistory(IResource resource, int flags, IProgressMonitor monitor, boolean returnAll) {
		this.resource = resource;
		this.flags = flags;
		this.returnAll = returnAll;
		String prefix = RepositoryMapping.getMapping(resource).getSubset();
		String[] prefixSegments = prefix!=null ? prefix.split("/") : new String[0];
		String[] resourceSegments = resource.getProjectRelativePath().segments(); 
		relativeResourceName = new String[prefixSegments.length + resourceSegments.length];
		System.arraycopy(prefixSegments, 0, relativeResourceName, 0, prefixSegments.length);
		System.arraycopy(resourceSegments, 0, relativeResourceName, prefixSegments.length, resourceSegments.length);
		if ((flags & IFileHistoryProvider.SINGLE_LINE_OF_DESCENT) == 0) {
			findSingleRevision(monitor);
		} else {
			try {
				findRevisions(monitor);
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}

	public IFileRevision[] getContributors(IFileRevision revision) {
		if (revision instanceof GitCommitFileRevision) {
			GitCommitFileRevision grevision = (GitCommitFileRevision)revision;
			ObjectId[] parents = grevision.getCommit().getParentIds();
			IFileRevision[] ret = new IFileRevision[parents.length];
			for (int i = 0; i < parents.length; ++i) {
				ret[i] = new GitCommitFileRevision(parents[i], grevision
						.getResource(), -1);
			}
			return ret;
		}
		return new IFileRevision[0];
	}

	public IFileRevision getFileRevision(String id) {
		if (id.equals(""))
			return new GitWorkspaceFileRevision(resource, -1);
		if (id.equals("Index"))
			return new GitIndexFileRevision(resource, 0);
		return new GitCommitFileRevision(ObjectId.fromString(id), resource, 0);
	}

	static class EclipseWalker extends TopologicalWalker {

		IResource resource;
		private final IProgressMonitor monitor;
		private Map<ObjectId, IFileRevision> revisions = new HashMap<ObjectId, IFileRevision>();

		EclipseWalker(Repository repository, Commit[] starts, String[] relativeResourceName,boolean leafIsBlob,IResource resource,boolean followMainOnly, Boolean merges, ObjectId lastActiveDiffId, boolean returnAll, IProgressMonitor monitor) {
			super(repository, starts, relativeResourceName, leafIsBlob, followMainOnly, merges, lastActiveDiffId, returnAll);
			this.resource = resource;
			this.monitor = monitor;
		}

		protected void collect(Commit commit, int count, int breadth) {
			super.collect(commit, count, breadth);
			if (commit.getCommitId().equals(ObjectId.zeroId()))
				revisions.put(commit.getCommitId(), new GitIndexFileRevision(resource, count));
			else
				revisions.put(commit.getCommitId(), new GitCommitFileRevision(commit.getCommitId(), resource, count));
		}

		public boolean isCancelled() {
			return monitor.isCanceled();
		}

		@Override
		public Collection collectHistory() {
			Collection rawList = super.collectHistory();
			List<IFileRevision> ret = new MappedList<ObjectId,IFileRevision>((List)rawList) {
				public IFileRevision map(ObjectId in) {
					GitFileRevision ret = (GitFileRevision)revisions.get(in);
					if (ret == null && isReturnAll())
						if (in.equals(ObjectId.zeroId()))
							ret = new GitIndexFileRevision(resource, -1);
						else
							ret = new GitCommitFileRevision(in, resource, -1);
					if (ret != null)
						ret.setLane(getLane(in));
					return ret;
				}
			};
			return ret;
		}

		@Override
		protected void record(ObjectId pred, ObjectId succ) {
			super.record(pred, succ);
			if (monitor.isCanceled()) {
				System.out.println("Cancelled");
				throw new CancellationException("history refresh cancelled");
			}
		}
	}

	public IFileRevision[] getFileRevisions() {
		return revisions.toArray(new IFileRevision[revisions.size()]);
	}

	/**
	 * @return the list of revisions. The list may be lazy-evaluated.
	 */
	public List<IFileRevision> getFileRevisionsList() {
		return revisions;
	}

	/**
	 * Get a single file revision suitable for quickdiff.
	 * 
	 * We have two modes. For a branch set up for Stacked Git that has a patch
	 * return the revision prior to the topmost patch, be it another patch or a
	 * normal Git Commit. This is the revision in HEAD^. Otherwise we return the
	 * revision in HEAD.
	 * @param monitor 
	 */
	private void findSingleRevision(IProgressMonitor monitor) {
		try {
			Repository repository = RepositoryMapping.getMapping(resource).getRepository();
			ObjectId id = repository.resolve("HEAD");
			Commit current = repository.mapCommit(id);
			if (repository.isStGitMode()) {
				ObjectId[] parentIds = current.getParentIds();
				if (parentIds != null && parentIds.length > 0)
					current = repository.mapCommit(parentIds[0]);
				else {
					revisions = Collections.emptyList();
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
				revisions = Collections.singletonList(
						(IFileRevision)new GitCommitFileRevision(current.getCommitId(), resource, -1));
			else
				revisions = Collections.emptyList();

		} catch (IOException e) {
			e.printStackTrace();
			revisions = Collections.emptyList();
		}
	}

	private void findRevisions(IProgressMonitor monitor) throws IOException {
		RepositoryProvider provider = RepositoryProvider.getProvider(resource
				.getProject());
		if (provider instanceof GitProvider) {
			GitWorkspaceFileRevision wsrevision = new GitWorkspaceFileRevision(resource, -1);

			long time0 = new Date().getTime();
			System.out.println("getting file history");
			SuperList<IFileRevision> ret = new SuperList<IFileRevision>();
			ObjectId activeDiffLeafId = null;
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			Repository repository = mapping.getRepository();
			if (!(resource instanceof IContainer)) {
				String relativeResourceNameString = mapping
						.getRepoRelativePath(resource);
				Entry entry = repository.getIndex().getEntry(
						relativeResourceNameString);
				if (entry != null)
					if (entry.isModified(mapping.getWorkDir(), entry.getSize() < 500000)) {
						activeDiffLeafId = ObjectId.zeroId();
						wsrevision = new GitWorkspaceFileRevision(resource, 0); // mark "interesting"
					} else {
						activeDiffLeafId = entry.getObjectId();
					}
				else
					activeDiffLeafId = ObjectId.zeroId();
			}

			Collection<IFileRevision> githistory;
			ObjectId head = repository.resolve("HEAD");
			if (head != null) {
				List<Commit> startList = new ArrayList<Commit>();

				startList.add(repository.mapCommit(head));
				for(String branch : repository.getBranches()) {
					Commit commit = repository.mapCommit(branch);
					if (commit != null)
						startList.add(commit);
				}
				Commit[] starts = startList.toArray(new Commit[startList.size()]);
				EclipseWalker walker = new EclipseWalker(
						repository,
						starts,
						relativeResourceName,
						resource.getType() == IResource.FILE,
						resource,
						(flags & IFileHistoryProvider.SINGLE_LINE_OF_DESCENT) == 0,
						null,
						activeDiffLeafId,
						returnAll,
						monitor);
				githistory = walker.collectHistory();
			} else {
				githistory = new ArrayList<IFileRevision>();
			}
			if (githistory.size() > 0) {
				if (resource.getType()==IResource.FILE) {
					if (returnAll)
						ret.add(wsrevision);
					ret.addAll(githistory);
				} else {
					ret.addAll(githistory);
				}
			} else {
				ret.add(wsrevision);
			}
			long time1 = new Date().getTime();
			System.out.println("got file history in " + (time1 - time0)
					/ 1000.0 + "s");

			revisions = ret;

		} else {
			revisions = Collections.emptyList();
		}
	}

	public IFileRevision[] getTargets(IFileRevision revision) {
		if (revision instanceof GitCommitFileRevision) {
			GitCommitFileRevision grevision = (GitCommitFileRevision) revision;
			ObjectId targetCommitId = grevision.getCommit().getCommitId();
			List<IFileRevision> ret = new ArrayList<IFileRevision>(4);
			for(IFileRevision r: revisions) {
				Commit ref = ((GitCommitFileRevision)r).getCommit();
				ObjectId[] parentIds = ref.getParentIds();
				for (int j = 0; j < parentIds.length; ++j) {
					if (parentIds[j].equals(targetCommitId)) {
						ret.add(r);
						break;
					}
				}
			}
			return ret.toArray(new IFileRevision[ret.size()]);
		}
		return new IFileRevision[0];
	}

	public Object getAdapter(Class adapter) {
		System.out.println("GitFileHistory.getAdapter "+adapter.getName());
		return null;
	}

}
