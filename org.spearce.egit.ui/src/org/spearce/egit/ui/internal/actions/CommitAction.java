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
	
	private ArrayList<IFile> notIndexed;
	private ArrayList<IFile> indexChanges;
	private ArrayList<IFile> files;

	private Commit previousCommit;

	private boolean amendAllowed ;
	private boolean amending;


	public void run(IAction act) {
		resetState();
		try {
			buildIndexHeadDiffList();
			buildFilesystemList();
		} catch (CoreException e) {
			return;
		} catch (IOException e) {
			return;
		}

		Repository repo = null;
		for (IProject proj : listProjects()) {
			Repository repository = RepositoryMapping.getMapping(proj).getRepository();
			if (repo == null)
				repo = repository;
			else if (repo != repository) {
				amendAllowed = false;
				break;
			}
		}

		
		if (files.isEmpty()) {
			if (amendAllowed) {
				boolean result = MessageDialog
				.openQuestion(wp.getSite().getShell(),
						"No files to commit",
				"No changed items were selected. Do you wish to amend the last commit?");
				if (!result)
					return;
				amending = true;
			} else {
				MessageDialog.openWarning(wp.getSite().getShell(), "No files to commit", "No changed items were selected.\n\nAmend is not possible as you have selected multiple repositories.");
				return;
			}
		}

		loadPreviousCommit();

		CommitDialog commitDialog = new CommitDialog(wp.getSite().getShell());
		commitDialog.setAmending(amending);
		commitDialog.setAmendAllowed(amendAllowed);
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

	private void resetState() {
		files = new ArrayList<IFile>();
		notIndexed = new ArrayList<IFile>();
		indexChanges = new ArrayList<IFile>();
		amendAllowed = true;
		amending = false;
		previousCommit = null;
	}

	private void loadPreviousCommit() {
		IProject project = ((IResource) rsrcList.get(0)).getProject();

		Repository repo = RepositoryMapping.getMapping(project).getRepository();
		try {
			ObjectId parentId = repo.resolve("HEAD");
			previousCommit = repo.mapCommit(parentId);
		} catch (IOException e) {
		}
	}

	private void performCommit(CommitDialog commitDialog, String commitMessage)
			throws IOException {
		// System.out.println("Commit Message: " + commitMessage);
		IFile[] selectedItems = commitDialog.getSelectedItems();

		HashMap<Repository, Tree> treeMap = new HashMap<Repository, Tree>();
		prepareTrees(selectedItems, treeMap);

		commitMessage = doCommits(commitDialog, commitMessage, treeMap);
		for (IProject proj : listProjects()) {
			RepositoryMapping.getMapping(proj).recomputeMerge();
		}
	}

	private String doCommits(CommitDialog commitDialog, String commitMessage,
			HashMap<Repository, Tree> treeMap) throws IOException {
		for (java.util.Map.Entry<Repository, Tree> entry : treeMap.entrySet()) {
			Tree tree = entry.getValue();
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
		}
		return commitMessage;
	}

	private void prepareTrees(IFile[] selectedItems,
			HashMap<Repository, Tree> treeMap) throws IOException,
			UnsupportedEncodingException {
		if (selectedItems.length == 0) {
			// amending commit - need to put something into the map
			for (IProject proj : listProjects()) {
				Repository repo = RepositoryMapping.getMapping(proj).getRepository();
				if (!treeMap.containsKey(repo))
					treeMap.put(repo, repo.mapTree("HEAD"));
			}
		}

		for (IFile file : selectedItems) {
			// System.out.println("\t" + file);

			IProject project = file.getProject();
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
			Repository repository = repositoryMapping.getRepository();
			Tree projTree = treeMap.get(repository);
			if (projTree == null) {
				projTree = repository.mapTree("HEAD");
				treeMap.put(repository, projTree);
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
			if (notIndexed.contains(file)) {
				File thisfile = new File(repositoryMapping.getWorkDir(), idxEntry.getName());
				if (!thisfile.isFile()) {
					index.remove(repositoryMapping.getWorkDir(), thisfile);
					index.write();
					System.out.println("Phantom file, so removing from index");
					continue;
				} else {
					if (idxEntry.update(thisfile))
						index.write();
				}
			}
				
			
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
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
			if (repositoryMapping != null) {
				Repository repository = repositoryMapping.getRepository();
				Tree head = repository.mapTree("HEAD");
				GitIndex index = repository.getIndex();
				IndexDiff indexDiff = new IndexDiff(head, index);
				indexDiff.diff();

				includeList(project, indexDiff.getAdded(), indexChanges);
				includeList(project, indexDiff.getChanged(), indexChanges);
				includeList(project, indexDiff.getRemoved(), indexChanges);
				includeList(project, indexDiff.getMissing(), notIndexed);
			}
		}
	}

	private boolean isRepositoryRootedInProject(IProject project) {
		RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
		File projectRoot = project.getLocation().toFile();
		File workDir = repositoryMapping.getWorkDir();

		return workDir.equals(projectRoot);
	}
	
	private void includeList(IProject project, HashSet<String> added, ArrayList<IFile> category) {
		for (String filename : added) {
			Path path = new Path(filename);
			try {
				IResource member;
				if (isRepositoryRootedInProject(project)) {
					member = project.getFile(path);
				} else {
					if (filename.startsWith(project.getFullPath().toFile().getName()))
						member = project.getWorkspace().getRoot().getFile(path);
					else continue;
				}

				if (member != null && member instanceof IFile) {
					if (!files.contains(member))
						files.add((IFile) member);
					category.add((IFile) member);
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

	private void buildFilesystemList() throws CoreException {
		for (final Iterator i = rsrcList.iterator(); i.hasNext();) {
			IResource resource = (IResource) i.next();
			final IProject project = resource.getProject();
			final GitProjectData projectData = GitProjectData.get(project);

			if (projectData != null) {

				if (resource instanceof IFile) {
					tryAddResource((IFile) resource, projectData, notIndexed);
				} else {
					resource.accept(new IResourceVisitor() {
						public boolean visit(IResource rsrc)
								throws CoreException {
							if (rsrc instanceof IFile) {
								tryAddResource((IFile) rsrc, projectData, notIndexed);
								return false;
							}
							return true;
						}
					});
				}
			}
		}
	}

	public boolean tryAddResource(IFile resource, GitProjectData projectData, ArrayList<IFile> category) {
		if (files.contains(resource))
			return false;

		try {
			RepositoryMapping repositoryMapping = projectData
					.getRepositoryMapping(resource.getProject());

			if (isChanged(repositoryMapping, resource)) {
				files.add(resource);
				category.add(resource);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean isChanged(RepositoryMapping map, IFile resource) {
		try {
			Repository repository = map.getRepository();
			GitIndex index = repository.getIndex();
			String repoRelativePath = map.getRepoRelativePath(resource);
			Entry entry = index.getEntry(repoRelativePath);
			if (entry != null)
				return entry.isModified(map.getWorkDir());
			return false;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
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
