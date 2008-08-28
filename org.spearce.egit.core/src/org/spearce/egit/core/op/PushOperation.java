/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.op;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.EclipseGitProgressTransformer;
import org.spearce.jgit.errors.NoRemoteRepositoryException;
import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.transport.PushResult;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.URIish;

/**
 * Push operation: pushing from local repository to one or many remote ones.
 */
public class PushOperation implements IRunnableWithProgress {
	private static final int WORK_UNITS_PER_TRANSPORT = 10;

	private final Repository localDb;

	private final PushOperationSpecification specification;

	private final boolean dryRun;

	private final RemoteConfig rc;

	private final PushOperationResult operationResult = new PushOperationResult();

	/**
	 * Create push operation for provided specification.
	 * <p>
	 * Operation is not performed within constructor,
	 * {@link #run(IProgressMonitor)} method must be called for that.
	 *
	 * @param localDb
	 *            local repository.
	 * @param specification
	 *            specification of ref updates for remote repositories.
	 * @param rc
	 *            optional remote config to apply on used transports. May be
	 *            null.
	 * @param dryRun
	 *            true if push operation should just check for possible result
	 *            and not really update remote refs, false otherwise - when push
	 *            should act normally.
	 */
	public PushOperation(final Repository localDb,
			final PushOperationSpecification specification,
			final boolean dryRun, final RemoteConfig rc) {
		this.localDb = localDb;
		this.specification = specification;
		this.dryRun = dryRun;
		this.rc = rc;
	}

	/**
	 * @return push operation result.
	 */
	public PushOperationResult getOperationResult() {
		return operationResult;
	}

	/**
	 * @return operation specification, as provided in constructor.
	 */
	public PushOperationSpecification getSpecification() {
		return specification;
	}

	/**
	 * Execute operation and store result. Operation is executed independently
	 * on each remote repository.
	 * <p>
	 *
	 * @throws InvocationTargetException
	 *             Cause of this exceptions may include
	 *             {@link TransportException}, {@link NotSupportedException} or
	 *             some unexpected {@link RuntimeException}.
	 * @see IRunnableWithProgress#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException {
		if (monitor == null)
			monitor = new NullProgressMonitor();

		final int totalWork = specification.getURIsNumber()
				* WORK_UNITS_PER_TRANSPORT;
		if (dryRun)
			monitor.beginTask(CoreText.PushOperation_taskNameDryRun, totalWork);
		else
			monitor.beginTask(CoreText.PushOperation_taskNameNormalRun,
					totalWork);

		for (final URIish uri : specification.getURIs()) {
			final SubProgressMonitor subMonitor = new SubProgressMonitor(
					monitor, WORK_UNITS_PER_TRANSPORT,
					SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
			Transport transport = null;
			try {
				if (monitor.isCanceled()) {
					operationResult.addOperationResult(uri,
							CoreText.PushOperation_resultCancelled);
					continue;
				}
				transport = Transport.open(localDb, uri);

				if (rc != null)
					transport.applyConfig(rc);
				transport.setDryRun(dryRun);
				final EclipseGitProgressTransformer gitSubMonitor = new EclipseGitProgressTransformer(
						subMonitor);
				final PushResult pr = transport.push(gitSubMonitor,
						specification.getRefUpdates(uri));
				operationResult.addOperationResult(uri, pr);
			} catch (final NoRemoteRepositoryException e) {
				operationResult.addOperationResult(uri, NLS.bind(
						CoreText.PushOperation_resultNoServiceError, e
								.getMessage()));
			} catch (final TransportException e) {
				operationResult.addOperationResult(uri, NLS.bind(
						CoreText.PushOperation_resultTransportError, e
								.getMessage()));
			} catch (final NotSupportedException e) {
				operationResult.addOperationResult(uri, NLS.bind(
						CoreText.PushOperation_resultNotSupported, e
								.getMessage()));
			} finally {
				if (transport != null) {
					transport.close();
				}
				// Dirty trick to get things always working.
				subMonitor.beginTask("", WORK_UNITS_PER_TRANSPORT); //$NON-NLS-1$
				subMonitor.done();
				subMonitor.done();
			}
		}
		monitor.done();
	}
}
