package org.spearce.egit.ui.internal.actions;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
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
			if (repositoryMapping != null && mapping.getRepository() != repositoryMapping.getRepository()) {
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
}