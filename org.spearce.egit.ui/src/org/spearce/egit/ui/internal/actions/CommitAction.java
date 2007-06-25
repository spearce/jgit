/*
 *  Copyright (C) 2007 David Watson <dwatson@mimvista.com>
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

package org.spearce.egit.ui.internal.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.internal.dialogs.CommitDialog;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.IndexDiff;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.RefLock;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryConfig;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.GitIndex.Entry;

public class CommitAction implements IObjectActionDelegate {

	private IWorkbenchPart wp;

	private List rsrcList;

	public void setActivePart(final IAction act, final IWorkbenchPart part) {
		wp = part;
	}

	public void run(IAction act) {
		files.clear();
		try {
			buildIndexHeadDiffList();
			if (false)
				buildList();
		} catch (CoreException e) {
			return;
		} catch (IOException e) {
			return;
		}
		if (files.isEmpty()) {
			boolean result = MessageDialog
					.openQuestion(wp.getSite().getShell(),
							"No files to commit",
							"No changed items were selected. Do you wish to amend the last commit?");
			if (!result)
				return;
			amending = true;
		}

		loadPreviousCommit();

		CommitDialog commitDialog = new CommitDialog(wp.getSite().getShell());
		commitDialog.setAmending(amending);
		commitDialog.setFileList(files);
		if (previousCommit != null)
			commitDialog.setPreviousCommitMessage(previousCommit.getMessage());

		if (commitDialog.open() != IDialogConstants.OK_ID)
			return;

		String commitMessage = commitDialog.getCommitMessage();
		amending = commitDialog.isAmending();
		try {
			performCommit(commitDialog, commitMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Commit previousCommit;

	private void loadPreviousCommit() {
		IProject project = ((IResource) rsrcList.get(0)).getProject();
		GitProjectData gitProjectData = GitProjectData.get(project);

		Repository repo = gitProjectData.getRepositoryMapping(project)
				.getRepository();
		try {
			ObjectId parentId = repo.resolve("HEAD");
			previousCommit = repo.mapCommit(parentId);
		} catch (IOException e) {
		}
	}

	private boolean amending = false;

	private void performCommit(CommitDialog commitDialog, String commitMessage)
			throws IOException {
		// System.out.println("Commit Message: " + commitMessage);
		IFile[] selectedItems = commitDialog.getSelectedItems();

		HashMap<IProject, Tree> treeMap = new HashMap<IProject, Tree>();
		prepareTrees(selectedItems, treeMap);

		commitMessage = doCommits(commitDialog, commitMessage, treeMap);
	}

	private String doCommits(CommitDialog commitDialog, String commitMessage,
			HashMap<IProject, Tree> treeMap) throws IOException {
		for (java.util.Map.Entry<IProject, Tree> entry : treeMap.entrySet()) {
			Tree tree = entry.getValue();
			RepositoryMapping repositoryMapping = GitProjectData.get(
					entry.getKey()).getRepositoryMapping(entry.getKey());

			Repository repo = tree.getRepository();
			writeTreeWithSubTrees(tree);

			ObjectId currentHeadId = repo.resolve("HEAD");
			ObjectId[] parentIds = new ObjectId[] { currentHeadId };
			if (amending) {
				parentIds = previousCommit.getParentIds();
			}
			Commit commit = new Commit(repo, parentIds);
			commit.setTree(tree);
			commitMessage = commitMessage.replaceAll("\r", "\n");

			RepositoryConfig config = repo.getConfig();
			String username = config.getString("user", "name");
			if (username == null)
				username = System.getProperty("user.name");

			String email = config.getString("user", "email");
			if (email == null)
				email = System.getProperty("user.name") + "@" + getHostName();

			if (commitDialog.isSignedOff()) {
				commitMessage += "\n\nSigned-off-by: " + username + " <"
						+ email + ">";
			}
			commit.setMessage(commitMessage);

			if (commitDialog.getAuthor() == null) {
				commit.setAuthor(new PersonIdent(username, email, new Date(
						Calendar.getInstance().getTimeInMillis()), TimeZone
						.getDefault()));
			} else {
				PersonIdent author = new PersonIdent(commitDialog.getAuthor());
				commit.setAuthor(new PersonIdent(author, new Date(Calendar
						.getInstance().getTimeInMillis()), TimeZone
						.getDefault()));
			}
			commit.setCommitter(new PersonIdent(username, email, new Date(
					Calendar.getInstance().getTimeInMillis()), TimeZone
					.getDefault()));

			ObjectWriter writer = new ObjectWriter(repo);
			commit.setCommitId(writer.writeCommit(commit));
			System.out.println("Commit iD: " + commit.getCommitId());

			RefLock lockRef = repo.lockRef("HEAD");
			lockRef.write(commit.getCommitId());
			if (lockRef.commit()) {
				System.out.println("Success!!!!");
				updateReflog(repo, commitMessage, currentHeadId, commit
						.getCommitId(), commit.getCommitter());
			}
			repositoryMapping.recomputeMerge();
		}
		return commitMessage;
	}

	private void prepareTrees(IFile[] selectedItems,
			HashMap<IProject, Tree> treeMap) throws IOException,
			UnsupportedEncodingException {
		if (selectedItems.length == 0) {
			// amending commit - need to put something into the map
			for (IProject proj : listProjects()) {
				treeMap.put(proj, GitProjectData.get(proj)
						.getRepositoryMapping(proj).getRepository().mapTree(
								"HEAD"));
			}
		}

		for (IFile file : selectedItems) {
			// System.out.println("\t" + file);

			IProject project = file.getProject();
			final GitProjectData projectData = GitProjectData.get(project);
			RepositoryMapping repositoryMapping = projectData
					.getRepositoryMapping(project);

			Tree projTree = treeMap.get(project);
			Repository repository = repositoryMapping.getRepository();
			if (projTree == null) {
				projTree = repository.mapTree("HEAD");
				treeMap.put(project, projTree);
				System.out.println("Orig tree id: " + projTree.getId());
			}
			GitIndex index = repository.getIndex();
			String repoRelativePath = repositoryMapping
					.getRepoRelativePath(file);
			String string = repoRelativePath;

			TreeEntry treeMember = projTree.findBlobMember(repoRelativePath);
			// we always want to delete it from the current tree, since if it's
			// updated, we'll add it again
			if (treeMember != null)
				treeMember.delete();

			Entry idxEntry = index.getEntry(string);
			if (idxEntry != null) {
				projTree.addFile(repoRelativePath);
				TreeEntry newMember = projTree.findBlobMember(repoRelativePath);

				newMember.setId(idxEntry.getObjectId());
				System.out.println("New member id for " + repoRelativePath
						+ ": " + newMember.getId() + " idx id: "
						+ idxEntry.getObjectId());
			}
		}
	}

	private String getHostName() {
		try {
			java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
			String hostname = addr.getCanonicalHostName();
			return hostname;
		} catch (java.net.UnknownHostException e) {
			return "localhost";
		}
	}

	private void updateReflog(Repository repo, String commitMessage,
			ObjectId parentId, ObjectId commitId, PersonIdent committer) {
		File headLog = new File(repo.getDirectory(), "logs/HEAD");
		writeReflog(commitMessage, parentId, commitId, committer, headLog);
		
		
		try {
			final File ptr = new File(repo.getDirectory(),"HEAD");
			final BufferedReader br = new BufferedReader(new FileReader(ptr));
			String ref;
			try {
				ref = br.readLine();
			} finally {
				br.close();
			}
			if (ref != null) {
				if (ref.startsWith("ref: "))
					ref = ref.substring(5);
				
				File branchLog = new File(repo.getDirectory(), "logs/" + ref);
				writeReflog(commitMessage, parentId, commitId, committer, branchLog);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeReflog(String commitMessage, ObjectId parentId,
			ObjectId commitId, PersonIdent committer, File file) {
		String firstLine = commitMessage;
		int newlineIndex = commitMessage.indexOf("\n");
		if (newlineIndex > 0) {
			firstLine = commitMessage.substring(0, newlineIndex);
		}
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileOutputStream(file, true));
			String commitStr = amending ? "\tcommit (amend):" : "\tcommit: ";
			out.println(parentId + " " + commitId + " "
					+ committer.toExternalString() + commitStr + firstLine);

		} catch (FileNotFoundException e) {
			System.out.println("Couldn't write reflog!");
		} finally {
			if (out != null)
				out.close();
		}
	}

	private void writeTreeWithSubTrees(Tree tree) {
		if (tree.getId() == null) {
			System.out.println("writing tree for: " + tree.getFullName());
			try {
				for (TreeEntry entry : tree.members()) {
					if (entry.isModified()) {
						if (entry instanceof Tree) {
							writeTreeWithSubTrees((Tree) entry);
						} else {
							// this shouldn't happen.... not quite sure what to
							// do here :)
							System.out.println("BAD JUJU: "
									+ entry.getFullName());
						}
					}
				}
				ObjectWriter writer = new ObjectWriter(tree.getRepository());
				tree.setId(writer.writeTree(tree));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void buildIndexHeadDiffList() throws IOException {
		for (IProject project : listProjects()) {
			final GitProjectData projectData = GitProjectData.get(project);
			if (projectData != null) {
				RepositoryMapping repositoryMapping = projectData
						.getRepositoryMapping(project);
				Repository repository = repositoryMapping.getRepository();
				Tree head = repository.mapTree("HEAD");
				GitIndex index = repository.getIndex();
				IndexDiff indexDiff = new IndexDiff(head, index);
				indexDiff.diff();

				includeList(project, indexDiff.getAdded());
				includeList(project, indexDiff.getChanged());
				includeList(project, indexDiff.getRemoved());
			}
		}
	}

	private void includeList(IProject project, HashSet<String> added) {
		for (String filename : added) {
			Path path = new Path(filename);
			try {
				IResource member = project.getWorkspace().getRoot().getFile(
						path);
				if (member == null)
					member = project.getFile(path);

				if (member != null && member instanceof IFile) {
					files.add((IFile) member);
				} else {
					System.out.println("Couldn't find " + filename);
				}
			} catch (Exception t) {
				continue;
			} // if it's outside the workspace, bad things happen
		}
	}

	private ArrayList<IProject> listProjects() {
		ArrayList<IProject> projects = new ArrayList<IProject>();

		for (Iterator i = rsrcList.iterator(); i.hasNext();) {
			IResource res = (IResource) i.next();
			if (!projects.contains(res.getProject()))
				projects.add(res.getProject());
		}
		return projects;
	}

	private ArrayList<IFile> files = new ArrayList<IFile>();

	private void buildList() throws CoreException {
		for (final Iterator i = rsrcList.iterator(); i.hasNext();) {
			IResource resource = (IResource) i.next();
			final IProject project = resource.getProject();
			final GitProjectData projectData = GitProjectData.get(project);

			if (projectData != null) {
				// final RepositoryMapping repositoryMapping =
				// projectData.getRepositoryMapping(project);
				// final Repository repository =
				// repositoryMapping.getRepository();

				if (resource instanceof IFile) {
					tryAddResource((IFile) resource, projectData);
				} else {
					resource.accept(new IResourceVisitor() {
						public boolean visit(IResource rsrc)
								throws CoreException {
							if (rsrc instanceof IFile) {
								tryAddResource((IFile) rsrc, projectData);
								return false;
							}
							return true;
						}
					});
				}
			}
		}
	}

	public void tryAddResource(IFile resource, GitProjectData projectData) {
		if (files.contains(resource))
			return;

		try {
			RepositoryMapping repositoryMapping = projectData
					.getRepositoryMapping(resource.getProject());

			if (repositoryMapping.isResourceChanged(resource))
				files.add(resource);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void selectionChanged(IAction act, ISelection sel) {
		final List selection;
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			selection = ((IStructuredSelection) sel).toList();
		} else {
			selection = Collections.EMPTY_LIST;
		}
		act.setEnabled(!selection.isEmpty());
		rsrcList = selection;
	}

}
