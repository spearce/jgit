/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.core.op;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.EclipseGitProgressTransformer;
import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.WorkDirCheckout;
import org.spearce.jgit.transport.FetchResult;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.Transport;

/**
 * Clones a repository from a remote location to a local location.
 */
public class CloneOperation implements IRunnableWithProgress {
	private final Repository local;

	private final RemoteConfig remote;

	private final String branch;

	private FetchResult fetchResult;

	/**
	 * Create a new clone operation.
	 * 
	 * @param r
	 *            repository the checkout will happen within.
	 * @param t
	 *            remote we should fetch from.
	 * @param b
	 *            branch to initially clone from.
	 */
	public CloneOperation(final Repository r, final RemoteConfig t,
			final String b) {
		local = r;
		remote = t;
		branch = b;
	}

	public void run(final IProgressMonitor pm) throws InvocationTargetException {
		final IProgressMonitor monitor;
		if (pm == null)
			monitor = new NullProgressMonitor();
		else
			monitor = pm;

		try {
			monitor.beginTask(NLS.bind(CoreText.CloneOperation_title, remote
					.getURIs().get(0).toString()), 5000);
			doFetch(new SubProgressMonitor(monitor, 4000));
			doCheckout(new SubProgressMonitor(monitor, 1000));
		} catch (IOException e) {
			if (!monitor.isCanceled())
				throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	private void doFetch(final IProgressMonitor monitor)
			throws NotSupportedException, TransportException {
		fetchResult = Transport.open(local, remote).fetch(
				new EclipseGitProgressTransformer(monitor), null);
	}

	private void doCheckout(final IProgressMonitor monitor) throws IOException {
		final Ref head = fetchResult.getAdvertisedRef(branch);
		if (head == null || head.getObjectId() == null)
			return;

		final GitIndex index = new GitIndex(local);
		final Commit mapCommit = local.mapCommit(head.getObjectId());
		final Tree tree = mapCommit.getTree();
		final RefUpdate u;
		final WorkDirCheckout co;

		u = local.updateRef(Constants.HEAD);
		u.setNewObjectId(mapCommit.getCommitId());
		u.forceUpdate();

		monitor.setTaskName("Checking out files");
		co = new WorkDirCheckout(local, local.getWorkDir(), index, tree);
		co.checkout();
		monitor.setTaskName("Writing index");
		index.write();
	}
}