/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * Copyright (c) 2003, 2006 Subclipse project and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/

package org.spearce.egit.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;

/**
 * A checked exception representing a failure in the Git plugin.
 * <p>
 * Git exceptions contain a status object describing the cause of the exception.
 * </p>
 *
 * @see IStatus
 */
public class GitException extends TeamException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new Git exception
	 *
	 * @param severity
	 * @param code
	 * @param message
	 * @param e
	 */
	public GitException(int severity, int code, String message, Throwable e) {
		super(new TeamStatus(severity, Activator.getPluginId(), code, message,
				e, null));
	}

	/**
	 * Constructs a new Git exception
	 *
	 * @param severity
	 * @param code
	 * @param message
	 */
	public GitException(int severity, int code, String message) {
		this(severity, code, message, null);
	}

	/**
	 * Constructs a new Git exception
	 *
	 * @param message
	 * @param e
	 */
	public GitException(String message, Throwable e) {
		this(IStatus.ERROR, UNABLE, message, e);
	}

	/**
	 * Constructs a new Git exception
	 *
	 * @param message
	 */
	public GitException(String message) {
		this(message, null);
	}

	/**
	 * Constructs a new Git exception
	 *
	 * @param status
	 */
	public GitException(IStatus status) {
		super(status);
	}

	/**
	 * Transform this exception into a CoreException
	 *
	 * @return the new CoreException
	 */
	public CoreException toCoreException() {
		IStatus status = getStatus();
		return new CoreException(new Status(status.getSeverity(), status
				.getPlugin(), 0, status.getMessage(), this));
	}

	/**
	 * Static helper method for creating a Git exception
	 *
	 * @param resource
	 * @param message
	 * @param e
	 * @return the created exception
	 */
	public static GitException wrapException(IResource resource,
			String message, CoreException e) {
		return new GitException(IStatus.ERROR, e.getStatus().getCode(),
				message, e);
	}

	/**
	 * Static helper method for creating a Git exception
	 *
	 * @param e
	 * @return the created exception
	 */
	public static GitException wrapException(Exception e) {
		Throwable t = e;
		if (e instanceof InvocationTargetException) {
			Throwable target = ((InvocationTargetException) e)
					.getTargetException();
			if (target instanceof GitException) {
				return (GitException) target;
			}
			t = target;
		}

		return new GitException(IStatus.ERROR, UNABLE,
				t.getMessage() != null ? t.getMessage() : "", t); //$NON-NLS-1$
	}

	/**
	 * Static helper method for creating a Git exception
	 *
	 * @param e
	 * @return the created exception
	 */
	public static GitException wrapException(CoreException e) {
		IStatus status = e.getStatus();
		if (!status.isMultiStatus()) {
			status = new TeamStatus(status.getSeverity(), Activator
					.getPluginId(), status.getCode(), status.getMessage(), e,
					null);
		}
		return new GitException(status);
	}

	/**
	 * Static helper method for creating a Git exception
	 *
	 * @param e
	 * @return the created exception
	 */
	public static GitException wrapException(IOException e) {
		return new GitException(IStatus.ERROR, IO_FAILED, e.getMessage(), e);
	}

	/**
	 * Static helper method for creating a Git exception
	 *
	 * @param e
	 * @return the created exception
	 */
	public static GitException wrapException(TeamException e) {
		if (e instanceof GitException)
			return (GitException) e;
		else
			return new GitException(e.getStatus());
	}
}
