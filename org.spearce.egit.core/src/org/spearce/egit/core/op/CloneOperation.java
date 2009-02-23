/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

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
import org.spearce.jgit.lib.RepositoryConfig;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.WorkDirCheckout;
import org.spearce.jgit.transport.FetchResult;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.URIish;

/**
 * Clones a repository from a remote location to a local location.
 */
public class CloneOperation implements IRunnableWithProgress {
	private final URIish uri;

	private final boolean allSelected;

	private final Collection<Ref> selectedBranches;

	private final File workdir;

	private final String branch;

	private final String remoteName;

	private Repository local;

	private RemoteConfig remoteConfig;

	private FetchResult fetchResult;

	/**
	 * Create a new clone operation.
	 * 
	 * @param uri
	 *            remote we should fetch from.
	 * @param allSelected
	 *            true when all branches have to be fetched (indicates wildcard
	 *            in created fetch refspec), false otherwise.
	 * @param selectedBranches
	 *            collection of branches to fetch. Ignored when allSelected is
	 *            true.
	 * @param workdir
	 *            working directory to clone to. The directory may or may not
	 *            already exist.
	 * @param branch
	 *            branch to initially clone from.
	 * @param remoteName
	 *            name of created remote config as source remote (typically
	 *            named "origin").
	 */
	public CloneOperation(final URIish uri, final boolean allSelected,
			final Collection<Ref> selectedBranches, final File workdir,
			final String branch, final String remoteName) {
		this.uri = uri;
		this.allSelected = allSelected;
		this.selectedBranches = selectedBranches;
		this.workdir = workdir;
		this.branch = branch;
		this.remoteName = remoteName;
	}

	public void run(final IProgressMonitor pm)
			throws InvocationTargetException, InterruptedException {
		final IProgressMonitor monitor;
		if (pm == null)
			monitor = new NullProgressMonitor();
		else
			monitor = pm;

		try {
			monitor.beginTask(NLS.bind(CoreText.CloneOperation_title, uri),
					5000);
			try {
				doInit(new SubProgressMonitor(monitor, 100));
				doFetch(new SubProgressMonitor(monitor, 4000));
				doCheckout(new SubProgressMonitor(monitor, 900));
			} finally {
				closeLocal();
			}
		} catch (final Exception e) {
			delete(workdir);
			if (monitor.isCanceled())
				throw new InterruptedException();
			else
				throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	private void closeLocal() {
		if (local != null) {
			local.close();
			local = null;
		}
	}

	private void doInit(final IProgressMonitor monitor)
			throws URISyntaxException, IOException {
		monitor.setTaskName("Initializing local repository");

		final File gitdir = new File(workdir, ".git");
		local = new Repository(gitdir);
		local.create();
		local.writeSymref(Constants.HEAD, branch);

		remoteConfig = new RemoteConfig(local.getConfig(), remoteName);
		remoteConfig.addURI(uri);

		final String dst = Constants.R_REMOTES + remoteConfig.getName();
		RefSpec wcrs = new RefSpec();
		wcrs = wcrs.setForceUpdate(true);
		wcrs = wcrs.setSourceDestination(Constants.R_HEADS + "*", dst + "/*");

		if (allSelected) {
			remoteConfig.addFetchRefSpec(wcrs);
		} else {
			for (final Ref ref : selectedBranches)
				if (wcrs.matchSource(ref))
					remoteConfig.addFetchRefSpec(wcrs.expandFromSource(ref));
		}

		// we're setting up for a clone with a checkout
		local.getConfig().setBoolean("core", null, "bare", false);

		remoteConfig.update(local.getConfig());

		// branch is like 'Constants.R_HEADS + branchName', we need only
		// the 'branchName' part
		String branchName = branch.substring(Constants.R_HEADS.length());

		// setup the default remote branch for branchName
		local.getConfig().setString(RepositoryConfig.BRANCH_SECTION,
				branchName, "remote", remoteName);
		local.getConfig().setString(RepositoryConfig.BRANCH_SECTION,
				branchName, "merge", branch);

		local.getConfig().save();
	}

	private void doFetch(final IProgressMonitor monitor)
			throws NotSupportedException, TransportException {
		final Transport tn = Transport.open(local, remoteConfig);
		try {
			final EclipseGitProgressTransformer pm;
			pm = new EclipseGitProgressTransformer(monitor);
			fetchResult = tn.fetch(pm, null);
		} finally {
			tn.close();
		}
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

	private static void delete(final File d) {
		if (d.isDirectory()) {
			final File[] items = d.listFiles();
			if (items != null) {
				for (final File c : items)
					delete(c);
			}
		}
		d.delete();
	}
}