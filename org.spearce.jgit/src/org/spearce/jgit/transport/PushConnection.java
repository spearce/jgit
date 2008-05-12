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
package org.spearce.jgit.transport;

/**
 * Lists known refs from the remote and sends objects to the remote.
 * <p>
 * A push connection typically connects to the <code>git-receive-pack</code>
 * service running where the remote repository is stored. This provides a
 * one-way object transfer service to copy objects from the local repository
 * into the remote repository, as well as a way to modify the refs stored by the
 * remote repository.
 * <p>
 * Instances of a PushConnection must be created by a {@link Transport} that
 * implements a specific object transfer protocol that both sides of the
 * connection understand.
 * <p>
 * PushConnection instances are not thread safe and may be accessed by only one
 * thread at a time.
 * 
 * @see Transport
 */
public abstract class PushConnection {
	/**
	 * Close any resources used by this connection.
	 * <p>
	 * If the remote repository is contacted by a network socket this method
	 * must close that network socket, disconnecting the two peers. If the
	 * remote repository is actually local (same system) this method must close
	 * any open file handles used to read the "remote" repository.
	 */
	public abstract void close();
}
