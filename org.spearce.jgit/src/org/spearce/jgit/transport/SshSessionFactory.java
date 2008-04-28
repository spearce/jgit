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

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Creates and destroys SSH connections to a remote system.
 * <p>
 * Different implementations of the session factory may be used to control
 * communicating with the end-user as well as reading their personal SSH
 * configuration settings, such as known hosts and private keys.
 * <p>
 * A {@link Session} must be returned to the factory that created it. Callers
 * are encouraged to retain the SshSessionFactory for the duration of the period
 * they are using the Session.
 */
public abstract class SshSessionFactory {
	private static SshSessionFactory INSTANCE = new DefaultSshSessionFactory();

	/**
	 * Get the currently configured JVM-wide factory.
	 * <p>
	 * A factory is always available. By default the factory will read from the
	 * user's <code>$HOME/.ssh</code> and assume OpenSSH compatibility.
	 * 
	 * @return factory the current factory for this JVM.
	 */
	public static SshSessionFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * Change the JVM-wide factory to a different implementation.
	 * 
	 * @param newFactory
	 *            factory for future sessions to be created through. If null the
	 *            default factory will be restored.s
	 */
	public static void setInstance(final SshSessionFactory newFactory) {
		if (newFactory != null)
			INSTANCE = newFactory;
		else
			INSTANCE = new DefaultSshSessionFactory();
	}

	/**
	 * Open (or reuse) a session to a host.
	 * <p>
	 * A reasonable UserInfo that can interact with the end-user (if necessary)
	 * is installed on the returned session by this method.
	 * <p>
	 * The caller must connect the session by invoking <code>connect()</code>
	 * if it has not already been connected.
	 * 
	 * @param user
	 *            username to authenticate as. If null a reasonable default must
	 *            be selected by the implementation. This may be
	 *            <code>System.getProperty("user.name")</code>.
	 * @param pass
	 *            optional user account password or passphrase. If not null a
	 *            UserInfo that supplies this value to the SSH library will be
	 *            configured.
	 * @param host
	 *            hostname (or IP address) to connect to. Must not be null.
	 * @param port
	 *            port number the server is listening for connections on. May be <=
	 *            0 to indicate the IANA registered port of 22 should be used.
	 * @return a session that can contact the remote host.
	 * @throws JSchException
	 *             the session could not be created.
	 */
	public abstract Session getSession(String user, String pass, String host,
			int port) throws JSchException;

	/**
	 * Close (or recycle) a session to a host.
	 * 
	 * @param session
	 *            a session previously obtained from this factory's
	 *            {@link #getSession(String,String, String, int)} method.s
	 */
	public void releaseSession(final Session session) {
		if (session.isConnected())
			session.disconnect();
	}
}
