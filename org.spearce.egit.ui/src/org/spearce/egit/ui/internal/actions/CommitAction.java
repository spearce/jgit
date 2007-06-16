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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.IndexDiff;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;

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
			MessageDialog.openWarning(wp.getSite().getShell(),
					"No files to commit", "No changed items were selected.");
			return;
		}

		CommitDialog commitDialog = new CommitDialog(wp.getSite().getShell());
		commitDialog.setFileList(files);
		if (commitDialog.open() != IDialogConstants.OK_ID)
			return;

		String commitMessage = commitDialog.getCommitMessage();
		System.out.println("Commit Message: " + commitMessage);
		IFile[] selectedItems = commitDialog.getSelectedItems();
		for (IFile file : selectedItems) {
			System.out.println("\t" + file);
		}
	}

	private void buildIndexHeadDiffList() throws IOException {
		for (IProject project : listProjects()) {
			final GitProjectData projectData = GitProjectData.get(project);
			if (projectData != null) {
				RepositoryMapping repositoryMapping = projectData.getRepositoryMapping(project);
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
				IResource member = project.getWorkspace().getRoot().getFile(path);
				if (member == null)
					member = project.getFile(path);

				if (member != null && member instanceof IFile) {
					files.add((IFile) member);
				} else {
					System.out.println("Couldn't find " + filename);
				}
			} catch (Exception t) { continue;} // if it's outside the workspace, bad things happen
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
		act.setEnabled(!selection.isEmpty()) ;
		rsrcList = selection;
	}

}
