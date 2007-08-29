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
package org.spearce.egit.core;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.GitIndex.Entry;

public class GitStorage implements IStorage {

	private final IResource resource;

	private ObjectId entry;

	private ObjectId treeId;

	public GitStorage(ObjectId treeId, IResource resource) {
		this.treeId = treeId;
		this.resource = resource;
		if (treeId == null)
			return;

		RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(resource);
		String name = repositoryMapping.getRepoRelativePath(resource);
		try {
			if (treeId.equals(ObjectId.zeroId())) {
				GitIndex index = repositoryMapping.getRepository().getIndex();
				Entry indexEntry = index.getEntry(name);
				if (indexEntry != null)
					entry = indexEntry.getObjectId();
			} else {
				Tree tree = repositoryMapping.getRepository().mapTree(treeId);
				TreeEntry treeEntry;
				if (resource.getType() == IResource.FILE)
					treeEntry = tree.findBlobMember(name);
				else
					treeEntry = tree.findTreeMember(name);
				if (treeEntry != null)
					entry = treeEntry.getId();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public InputStream getContents() throws CoreException {
		try {
			if (treeId == null) {
				return ((IFile) resource).getContents();
			} else {
				if (entry == null)
					return new ByteArrayInputStream(new byte[0]);
				else {
					ObjectLoader reader = RepositoryMapping.getMapping(
							resource.getProject()).getRepository().openBlob(
							entry);
					byte[] bytes = reader.getBytes();
					return new ByteArrayInputStream(bytes);
				}
			}
		} catch (FileNotFoundException e) {
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL,
					resource.getFullPath(), "Could not read file", e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ResourceException(33, resource.getFullPath(), e
					.getMessage(), e);
		}
	}

	public IPath getFullPath() {
		return resource.getFullPath();
	}

	public String getName() {
		return resource.getName();
	}

	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

}
