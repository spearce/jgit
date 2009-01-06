/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.Repository;

/**
 * A helper class for Team Actions on Git controlled projects
 */
public abstract class RepositoryAction extends TeamAction {

	// There are changes in Eclipse 3.3 requiring that execute be implemented
	// for it to compile. while 3.2 requires that run is implemented instead.
	/*
	 * See {@link #run(IAction)}
	 *
	 * @param action
	 */
	public void execute(IAction action) {
		run(action);
	}

	/**
	 * @return the projects hosting the selected resources
	 */
	protected IProject[] getProjectsForSelectedResources() {
		Set<IProject> ret = new HashSet<IProject>();
		for (IResource resource : (IResource[])getSelectedAdaptables(getSelection(), IResource.class))
			ret.add(resource.getProject());
		return ret.toArray(new IProject[ret.size()]);
	}

	/**
	 * @param projects
	 *            a list of projects
	 * @return the repositories that projects map to iff all projects are mapped
	 */ 
	protected Repository[] getRepositoriesFor(final IProject[] projects) {
		Set<Repository> ret = new HashSet<Repository>();
		for (IProject project : projects) {
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
			if (repositoryMapping == null)
				return new Repository[0];
			ret.add(repositoryMapping.getRepository());
		}
		return ret.toArray(new Repository[ret.size()]);
	}
	
	/**
	 * List the projects with selected resources, if all projects are connected
	 * to a Git repository.
	 * 
	 * @return the tracked projects affected by the current resource selection
	 */
	public IProject[] getProjectsInRepositoryOfSelectedResources() {
		Set<IProject> ret = new HashSet<IProject>();
		Repository[] repositories = getRepositoriesFor(getProjectsForSelectedResources());
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			for (Repository repository : repositories) {
				if (mapping != null && mapping.getRepository() == repository) {
					ret.add(project);
					break;
				}
			}
		}
		return ret.toArray(new IProject[ret.size()]);
	}

	/**
	 * Figure out which repository to use. All selected
	 * resources must map to the same Git repository.
	 *
	 * @param warn Put up a message dialog to warn why a resource was not selected
	 * @return repository for current project, or null
	 */
	protected Repository getRepository(boolean warn) {
		RepositoryMapping mapping = null;
		for (IProject project : getSelectedProjects()) {
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
			if (mapping == null) 
				mapping = repositoryMapping;
			if (repositoryMapping == null)
				return null;
			if (mapping.getRepository() != repositoryMapping.getRepository()) {
				if (warn)
					MessageDialog.openError(getShell(), "Multiple Repositories Selection", "Cannot perform reset on multiple repositories simultaneously.\n\nPlease select items from only one repository.");
				return null;
			}
		}
		if (mapping == null) {
			if (warn)
				MessageDialog.openError(getShell(), "Cannot Find Repository", "Could not find a repository associated with this project");
			return null;
		}
		
		final Repository repository = mapping.getRepository();
		return repository;
	}

	/**
	 * Figure out which repositories to use. All selected
	 * resources must map to a Git repository.
	 *
	 * @return repository for current project, or null
	 */
	protected Repository[] getRepositories() {
		IProject[] selectedProjects = getSelectedProjects();
		Set<Repository> repos = new HashSet<Repository>(selectedProjects.length);
		for (IProject project : selectedProjects) {
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
			if (repositoryMapping == null)
				return new Repository[0];
			repos.add(repositoryMapping.getRepository());
		}
		return repos.toArray(new Repository[repos.size()]);
	}

	// Re-make isEnabled abstract
	@Override
	abstract public boolean isEnabled();
}