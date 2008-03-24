package org.spearce.egit.ui.internal.history;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommit;

class BlobFileRevision extends FileRevision {
	private final Repository db;

	private final String path;

	private final ObjectId blobId;

	private final RevCommit commit;

	private PersonIdent author;

	BlobFileRevision(final Repository r, final RevCommit c, final String p,
			final ObjectId bId) {
		db = r;
		path = p;
		blobId = bId;
		commit = c;
	}

	public String getName() {
		return path;
	}

	@Override
	public String getAuthor() {
		return author().getName();
	}

	@Override
	public long getTimestamp() {
		return author().getWhen().getTime();
	}

	@Override
	public String getContentIdentifier() {
		return blobId.toString().substring(0, 8);
	}

	@Override
	public String getComment() {
		return getContentIdentifier();
	}

	private PersonIdent author() {
		if (author == null)
			author = commit.getAuthorIdent();
		return author;
	}

	public IStorage getStorage(final IProgressMonitor monitor)
			throws CoreException {
		return new IStorage() {
			public InputStream getContents() throws CoreException {
				try {
					final ObjectLoader ldr = db.openBlob(blobId);
					final byte[] data;

					data = ldr != null ? ldr.getBytes() : new byte[0];
					return new ByteArrayInputStream(data);
				} catch (IOException ioe) {
					return new ByteArrayInputStream(new byte[0]);
				}
			}

			public IPath getFullPath() {
				return Path.fromPortableString(path);
			}

			public String getName() {
				return path;
			}

			public boolean isReadOnly() {
				return true;
			}

			public Object getAdapter(final Class adapter) {
				return null;
			}
		};
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(final IProgressMonitor monitor)
			throws CoreException {
		return null;
	}
}
