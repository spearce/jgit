/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.internal.storage;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;

/** An {@link IFileRevision} for the current version in the workspace. */
class WorkspaceFileRevision extends GitFileRevision implements IFileRevision {
	private final IResource rsrc;

	WorkspaceFileRevision(final IResource resource) {
		super(resource.getName());
		rsrc = resource;
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		return rsrc instanceof IStorage ? (IStorage) rsrc : null;
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(IProgressMonitor monitor)
			throws CoreException {
		return null;
	}

	public String getAuthor() {
		return "";
	}

	public long getTimestamp() {
		return -1;
	}

	public String getComment() {
		return "";
	}

	public String getContentIdentifier() {
		return WORKSPACE;
	}
}
