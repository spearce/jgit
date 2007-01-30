/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
