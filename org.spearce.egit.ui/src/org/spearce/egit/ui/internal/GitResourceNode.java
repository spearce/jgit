package org.spearce.egit.ui.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.history.IFileRevision;
import org.spearce.egit.core.internal.mapping.GitCommitFileRevision;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

/**
 * A resource node is for letting Eclipse access data in the git repo in a hierachical
 * fashion, e.g. for the compare editor.
 */
public class GitResourceNode extends BufferedContent implements IStructureComparator, ITypedElement {
	TreeEntry entry;
	GitResourceNode[] children;
	String contentIdentifier;

	/**
	 * Construct a resource not for a {@link TreeEntry}
	 * @param e The {@link TreeEntry}
	 */
	public GitResourceNode(TreeEntry e) {
		entry = e;
	}

	/**
	 * Construct a {@link GitResourceNode} for a resource ties a specific historical version
	 * @param file
	 */
	public GitResourceNode(IFileRevision file) {
		this(file instanceof GitCommitFileRevision ? ((GitCommitFileRevision)file).getTreeEntry() : null);
		contentIdentifier = ((GitCommitFileRevision)file).getContentIdentifier();
	}

	public Object[] getChildren() {
		if (children != null)
			return children;
		if (entry instanceof Tree) {
			try {
				Tree t = (Tree)entry;
				children = new GitResourceNode[t.memberCount()];
				for (int i=0; i<children.length; ++i) {
					children[i] = new GitResourceNode(t.members()[i]);
				}
			} catch (IOException e) {
				// TODO: eclipse error handling
				e.printStackTrace();
				children = new GitResourceNode[0];
			}
		}
		return children;
	}

	public boolean equals(Object obj) {
		return entry.getId().equals(((GitResourceNode)obj).entry.getId());
	}

	protected InputStream createStream() throws CoreException {
		if (entry instanceof FileTreeEntry) {
			try {
				ObjectId id = entry.getId();
				ObjectLoader reader = entry.getRepository().openBlob(id);
				byte[] bytes = reader.getBytes();
				return new ByteArrayInputStream(bytes);
			} catch (IOException e) {
				// TODO: eclipse error handling
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	public String getName() {
		if (entry != null)
			return entry.getFullName();
		else
			return "<none>";
	}

	public Image getImage() {
		return CompareUI.getImage(getType());
	}

	public String getType() {
		if (entry instanceof Tree)
			return ITypedElement.FOLDER_TYPE;
		else {
			if (entry != null) {
				String name = entry.getName();
				if (name != null) {
					int index = name.lastIndexOf('.');
					if (index == -1)
						return ""; //$NON-NLS-1$
					if (index == (name.length() - 1))
						return ""; //$NON-NLS-1$
					return name.substring(index + 1);
				}
				return "";
			} else
				return "";
		}
	}

	/**
	 * @return a user friendly version identification of the resource
	 */
	public String getContentIdentifier() {
		return contentIdentifier;
	}
}

