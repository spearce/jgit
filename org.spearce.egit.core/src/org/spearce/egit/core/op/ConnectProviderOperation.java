/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryFinder;
import org.spearce.egit.core.project.RepositoryMapping;

/**
 * Connects Eclipse to an existing Git repository
 */
public class ConnectProviderOperation implements IWorkspaceRunnable {
	private final IProject[] projects;

	/**
	 * Create a new connection operation to execute within the workspace.
	 * 
	 * @param proj
	 *            the project to connect to the Git team provider.
	 */
	public ConnectProviderOperation(final IProject proj) {
		this(new IProject[] { proj });
	}

	/**
	 * Create a new connection operation to execute within the workspace.
	 *
	 * @param projects
	 *            the projects to connect to the Git team provider.
	 */
	public ConnectProviderOperation(final IProject[] projects) {
		this.projects = projects;
	}

	public void run(IProgressMonitor m) throws CoreException {
		if (m == null) {
			m = new NullProgressMonitor();
		}

		m.beginTask(CoreText.ConnectProviderOperation_connecting,
				100 * projects.length);
		try {

			for (IProject project : projects) {
				m.setTaskName(NLS.bind(
						CoreText.ConnectProviderOperation_ConnectingProject,
						project.getName()));
				Activator.trace("Locating repository for " + project); //$NON-NLS-1$
				Collection<RepositoryMapping> repos = new RepositoryFinder(
						project).find(new SubProgressMonitor(m, 40));
				if (repos.size() == 1) {
					GitProjectData projectData = new GitProjectData(project);
					try {
						projectData.setRepositoryMappings(repos);
						projectData.store();
					} catch (CoreException ce) {
						GitProjectData.delete(project);
						throw ce;
					} catch (RuntimeException ce) {
						GitProjectData.delete(project);
						throw ce;
					}
					RepositoryProvider
							.map(project, GitProvider.class.getName());
					projectData = GitProjectData.get(project);
					project.refreshLocal(IResource.DEPTH_INFINITE,
							new SubProgressMonitor(m, 50));
					m.worked(10);
				} else {
					Activator
							.trace("Attempted to share project without repository ignored :" //$NON-NLS-1$
									+ project);
					m.worked(60);
				}
			}
		} finally {
			m.done();
		}
	}
}
