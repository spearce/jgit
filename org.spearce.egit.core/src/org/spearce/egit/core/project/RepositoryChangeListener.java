/*******************************************************************************
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.project;

/**
 * Receives notification of a repository change event.
 * <p>
 * A change listener may be called from any thread, especially background job
 * threads, but also from the UI thread. Implementors are encouraged to complete
 * quickly, and make arrange for their tasks to run on the UI event thread if
 * necessary.
 * </p>
 */
public interface RepositoryChangeListener {
	/**
	 * Invoked when a repository has had some or all of its contents change.
	 * 
	 * @param which
	 *            the affected repository. Never null.
	 */
	public void repositoryChanged(RepositoryMapping which);
}
