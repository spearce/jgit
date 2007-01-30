/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.egit.core.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.spearce.egit.core.CoreText;

/**
 * Searches for existing Git repositories associated with a project's files.
 * <p>
 * This finder algorithm searches a project's contained files to see if any of
 * them are located within the working directory of an existing Git repository.
 * The finder searches through linked resources, as the EGit core is capable of
 * dealing with linked directories spanning multiple repositories in the same
 * project.
 * </p>
 * <p>
 * The search algorithm is exhaustive, it will find all matching repositories.
 * For the project itself as well as for each linked container within the
 * project it scans down the local filesystem trees to locate any Git
 * repositories which may be found there. It also scans up the local filesystem
 * tree to locate any Git repository which may be outside of Eclipse's
 * workspace-view of the world, but which contains the project or a linked
 * resource within the project. In short, if there is a Git repository
 * associated, it finds it.
 * </p>
 */
public class RepositoryFinder {
	private final IProject proj;

	private final Collection results;

	/**
	 * Create a new finder to locate Git repositories for a project.
	 * 
	 * @param p
	 *            the project this new finder should locate the existing Git
	 *            repositories of.
	 */
	public RepositoryFinder(final IProject p) {
		proj = p;
		results = new ArrayList();
	}

	/**
	 * Run the search algorithm.
	 * 
	 * @param m
	 *            a progress monitor to report feedback to; may be null.
	 * @return all found {@link RepositoryMapping} instances associated with the
	 *         project supplied to this instance's constructor.
	 * @throws CoreException
	 *             Eclipse was unable to access its workspace, and threw up on
	 *             us. We're throwing it back at the caller.
	 */
	public Collection find(IProgressMonitor m) throws CoreException {
		if (m == null) {
			m = new NullProgressMonitor();
		}
		find(m, proj);
		return results;
	}

	private void find(final IProgressMonitor m, final IContainer c)
			throws CoreException {
		final IPath loc = c.getLocation();

		m.beginTask("", 101);
		m.subTask(CoreText.RepositoryFinder_finding);
		try {
			if (loc != null) {
				final File fsLoc = loc.toFile();
				final File ownCfg = configFor(fsLoc);
				final IResource[] children;

				if (ownCfg.isFile()) {
					register(c, ownCfg.getParentFile(), null);
				} else if (c.isLinked() || c instanceof IProject) {
					String s = fsLoc.getName();
					File p = fsLoc.getParentFile();
					while (p != null) {
						final File pCfg = configFor(p);
						if (pCfg.isFile()) {
							register(c, pCfg.getParentFile(), s);
							break;
						}
						s = fsLoc.getName() + "/" + s;
						p = p.getParentFile();
					}
				}
				m.worked(1);

				children = c.members();
				if (children != null && children.length > 0) {
					final int scale = 100 / children.length;
					for (int k = 0; k < children.length; k++) {
						final IResource o = children[k];
						if (o instanceof IContainer
								&& !o.getName().equals(".git")) {
							find(new SubProgressMonitor(m, scale),
									(IContainer) o);
						} else {
							m.worked(scale);
						}
					}
				}
			}
		} finally {
			m.done();
		}
	}

	private File configFor(final File fsLoc) {
		return new File(new File(fsLoc, ".git"), "config");
	}

	private void register(final IContainer c, final File gitdir,
			final String subset) {
		File f;
		try {
			f = gitdir.getCanonicalFile();
		} catch (IOException ioe) {
			f = gitdir.getAbsoluteFile();
		}
		results.add(new RepositoryMapping(c, f, subset));
	}
}
