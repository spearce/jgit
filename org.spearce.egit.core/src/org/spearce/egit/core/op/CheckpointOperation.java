/*
 *  Copyright (C) 2006  Robin Rosenberg <robin.rosenberg@dewire.com>
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
package org.spearce.egit.core.op;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.spearce.egit.core.project.GitProjectData;

public class CheckpointOperation implements IWorkspaceRunnable {
    private final Collection rsrcList;

    public CheckpointOperation(final Collection rsrcs) {
	rsrcList = rsrcs;
    }

    public void run(IProgressMonitor m) throws CoreException {
	Set projects = new HashSet();
	for (Iterator i = rsrcList.iterator(); i.hasNext();) {
	    IResource r = (IResource) i.next();
	    IProject p = r.getProject();
	    projects.add(p);
	}
	for (Iterator i = projects.iterator(); i.hasNext();) {
	    IProject project = (IProject) i.next();
	    GitProjectData projectData = GitProjectData.get(project);
	    projectData.fullUpdate();
	}
    }
}
