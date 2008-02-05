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

import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.TopologicalSorter;

/**
 * A Git related {@link IFileRevision}. It references a version
 * and a resource, i.e. the version we think corresponds to the
 * resource in specific version.
 */
public abstract class GitFileRevision extends FileRevision {

	private final IResource resource;

	private final int count;

	private TopologicalSorter<ObjectId>.Lane lane;

	/**
	 * Construct a {@link GitFileRevision}
	 *
	 * @param resource
	 * @param count index into the full list of commits
	 */
	public GitFileRevision(IResource resource, int count) {
		this.count = count;
		this.resource = resource;
	}

	public String getName() {
		return resource.toString();
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(IProgressMonitor monitor)
			throws CoreException {
		return this;
	}

	public URI getURI() {
		return resource.getLocationURI();
	}

	/**
	 * @return the {@link IResource} the {@link IFileRevision} refers to.
	 */
	public IResource getResource() {
		return resource;
	}

	/**
	 * @deprecated
	 * @return index into full list of versions
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @return the swim lane assigned by the graph layout
	 */
	public TopologicalSorter<ObjectId>.Lane getLane() {
		return lane;
	}

	/**
	 * Set the swim where this revision is found. Invoked by the
	 * graph layout.
	 *
	 * @param lane
	 */
	public void setLane(TopologicalSorter<ObjectId>.Lane lane) {
		this.lane = lane;
	}

	/**
	 * @return the ObjectId this IFileRevision refers to.
	 */
	public ObjectId getCommitId() {
		return null;
	}

}
