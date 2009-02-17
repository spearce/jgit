/*******************************************************************************
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/

package org.spearce.egit.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.treewalk.AbstractTreeIterator;
import org.spearce.jgit.treewalk.WorkingTreeIterator;
import org.spearce.jgit.util.FS;

/**
 * Adapts an Eclipse {@link IContainer} for use in a <code>TreeWalk</code>.
 * <p>
 * This iterator converts an Eclipse IContainer object into something that a
 * TreeWalk instance can iterate over in parallel with any other Git tree data
 * structure, such as another working directory tree from outside of the
 * workspace or a stored tree from a Repository object database.
 * <p>
 * Modification times provided by this iterator are obtained from the cache
 * Eclipse uses to track external resource modification. This can be faster, but
 * requires the user refresh their workspace when external modifications take
 * place. This is not really a concern as it is common practice to need to do a
 * workspace refresh after externally modifying a file.
 *
 * @see org.spearce.jgit.treewalk.TreeWalk
 */
public class ContainerTreeIterator extends WorkingTreeIterator {
	private static String computePrefix(final IContainer base) {
		final RepositoryMapping rm = RepositoryMapping.getMapping(base);
		if (rm == null)
			throw new IllegalArgumentException("Not in a Git project: " + base);
		return rm.getRepoRelativePath(base);
	}

	private final IContainer node;

	/**
	 * Construct a new iterator from a container in the workspace.
	 * <p>
	 * The iterator will support traversal over the named container, but only if
	 * it is contained within a project which has the Git repository provider
	 * connected and this resource is mapped into a Git repository. During the
	 * iteration the paths will be automatically generated to match the proper
	 * repository paths for this container's children.
	 *
	 * @param base
	 *            the part of the workspace the iterator will walk over.
	 */
	public ContainerTreeIterator(final IContainer base) {
		super(computePrefix(base));
		node = base;
		init(entries());
	}

	/**
	 * Construct a new iterator from the workspace root.
	 * <p>
	 * The iterator will support traversal over workspace projects that have
	 * a Git repository provider connected and is mapped into a Git repository.
	 * During the iteration the paths will be automatically generated to match
	 * the proper repository paths for this container's children.
	 *
	 * @param root
	 *            the workspace root to walk over.
	 */
	public ContainerTreeIterator(final IWorkspaceRoot root) {
		super("");
		node = root;
		init(entries());
	}

	/**
	 * Construct a new iterator from a container in the workspace, with a given
	 * parent iterator.
	 * <p>
	 * The iterator will support traversal over the named container, but only if
	 * it is contained within a project which has the Git repository provider
	 * connected and this resource is mapped into a Git repository. During the
	 * iteration the paths will be automatically generated to match the proper
	 * repository paths for this container's children.
	 *
	 * @param p
	 *            the parent iterator we were created from.
	 * @param base
	 *            the part of the workspace the iterator will walk over.
	 */
	public ContainerTreeIterator(final WorkingTreeIterator p,
			final IContainer base) {
		super(p);
		node = base;
		init(entries());
	}

	@Override
	public AbstractTreeIterator createSubtreeIterator(final Repository db)
			throws IncorrectObjectTypeException, IOException {
		if (FileMode.TREE.equals(mode))
			return new ContainerTreeIterator(this,
					(IContainer) ((ResourceEntry) current()).rsrc);
		else
			throw new IncorrectObjectTypeException(ObjectId.zeroId(),
					Constants.TYPE_TREE);
	}

	/**
	 * Get the ResourceEntry for the current entry.
	 *
	 * @return the current entry
	 */
	public ResourceEntry getResourceEntry() {
		return (ResourceEntry) current();
	}

	private Entry[] entries() {
		final IResource[] all;
		try {
			all = node.members(IContainer.INCLUDE_HIDDEN);
		} catch (CoreException err) {
			return EOF;
		}

		final Entry[] r = new Entry[all.length];
		for (int i = 0; i < r.length; i++)
			r[i] = new ResourceEntry(all[i]);
		return r;
	}

	/**
	 * Wrapper for a resource in the Eclipse workspace
	 */
	static public class ResourceEntry extends Entry {
		final IResource rsrc;

		private final FileMode mode;

		private long length = -1;

		ResourceEntry(final IResource f) {
			rsrc = f;

			switch (f.getType()) {
			case IResource.FILE:
				if (FS.INSTANCE.canExecute(asFile()))
					mode = FileMode.EXECUTABLE_FILE;
				else
					mode = FileMode.REGULAR_FILE;
				break;
			case IResource.PROJECT:
			case IResource.FOLDER: {
				final IContainer c = (IContainer) f;
				if (c.findMember(".git") != null)
					mode = FileMode.GITLINK;
				else
					mode = FileMode.TREE;
				break;
			}
			default:
				mode = FileMode.MISSING;
				break;
			}
		}

		@Override
		public FileMode getMode() {
			return mode;
		}

		@Override
		public String getName() {
			if (rsrc.getType() == IResource.PROJECT)
				return rsrc.getLocation().lastSegment();
			else
				return rsrc.getName();
		}

		@Override
		public long getLength() {
			if (length < 0) {
				if (rsrc instanceof IFile)
					length = asFile().length();
				else
					length = 0;
			}
			return length;
		}

		@Override
		public long getLastModified() {
			return rsrc.getLocalTimeStamp();
		}

		@Override
		public InputStream openInputStream() throws IOException {
			if (rsrc instanceof IFile) {
				try {
					return ((IFile) rsrc).getContents(true);
				} catch (CoreException err) {
					final IOException ioe = new IOException(err.getMessage());
					ioe.initCause(err);
					throw ioe;
				}
			}
			throw new IOException("Not a regular file: " + rsrc);
		}

		/**
		 * Get the underlying resource of this entry.
		 *
		 * @return the underlying resource
		 */
		public IResource getResource() {
			return rsrc;
		}

		private File asFile() {
			return ((IFile) rsrc).getLocation().toFile();
		}
	}
}
