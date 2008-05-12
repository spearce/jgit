/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.egit.ui;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.ui.UserInfoPrompter;
import org.spearce.jgit.transport.SshSessionFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

class EclipseSshSessionFactory extends SshSessionFactory {
	private final IJSchService provider;

	EclipseSshSessionFactory(final IJSchService p) {
		provider = p;
	}

	@Override
	public Session getSession(final String user, final String pass,
			final String host, final int port) throws JSchException {
		final Session session = provider.createSession(host, port > 0 ? port
				: -1, user != null ? user : userName());
		if (pass != null)
			session.setPassword(pass);
		else
			new UserInfoPrompter(session);
		return session;
	}

	private static String userName() {
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return System.getProperty("user.name");
			}
		});
	}
}
