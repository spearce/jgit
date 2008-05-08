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

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Repository;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Transport through an SSH tunnel.
 * <p>
 * The SSH transport requires the remote side to have Git installed, as the
 * transport logs into the remote system and executes a Git helper program on
 * the remote side to read (or write) the remote repository's files.
 * <p>
 * This transport does not support direct SCP style of copying files, as it
 * assumes there are Git specific smarts on the remote side to perform object
 * enumeration, save file modification and hook execution.
 */
class TransportGitSsh extends PackTransport {
	static boolean canHandle(final URIish uri) {
		if (!uri.isRemote())
			return false;
		final String scheme = uri.getScheme();
		if ("ssh".equals(scheme))
			return true;
		if ("ssh+git".equals(scheme))
			return true;
		if ("git+ssh".equals(scheme))
			return true;
		if (scheme == null && uri.getHost() != null && uri.getPath() != null)
			return true;
		return false;
	}

	final SshSessionFactory sch;

	TransportGitSsh(final Repository local, final URIish uri) {
		super(local, uri);
		sch = SshSessionFactory.getInstance();
	}

	@Override
	public FetchConnection openFetch() throws TransportException {
		return new SshFetchConnection();
	}

	private static void sq(final StringBuilder cmd, final String val) {
		int i = 0;

		if (val.length() == 0)
			return;
		if (val.matches("^~[A-Za-z0-9_-]+$")) {
			// If the string is just "~user" we can assume they
			// mean "~user/" and evaluate it within the shell.
			//
			cmd.append(val);
			cmd.append('/');
			return;
		}

		if (val.matches("^~[A-Za-z0-9_-]*/.*$")) {
			// If the string is of "~/path" or "~user/path"
			// we must not escape ~/ or ~user/ from the shell
			// as we need that portion to be evaluated.
			//
			i = val.indexOf('/') + 1;
			cmd.append(val.substring(0, i));
			if (i == val.length())
				return;
		}

		cmd.append('\'');
		for (; i < val.length(); i++) {
			final char c = val.charAt(i);
			if (c == '\'')
				cmd.append("'\\''");
			else if (c == '!')
				cmd.append("'\\!'");
			else
				cmd.append(c);
		}
		cmd.append('\'');
	}

	Session openSession() throws TransportException {
		final String user = uri.getUser();
		final String pass = uri.getPass();
		final String host = uri.getHost();
		final int port = uri.getPort();
		try {
			final Session session;
			session = sch.getSession(user, pass, host, port);
			if (!session.isConnected())
				session.connect();
			return session;
		} catch (JSchException je) {
			final String us = uri.toString();
			final Throwable c = je.getCause();
			if (c instanceof UnknownHostException)
				throw new TransportException(us + ": Unknown host");
			if (c instanceof ConnectException)
				throw new TransportException(us + ": " + c.getMessage());
			throw new TransportException(us + ": " + je.getMessage(), je);
		}
	}

	ChannelExec exec(final Session sock, final String exe)
			throws TransportException {
		try {
			final ChannelExec channel = (ChannelExec) sock.openChannel("exec");
			String path = uri.getPath();
			if (uri.getScheme() != null && uri.getPath().startsWith("/~"))
				path = (uri.getPath().substring(1));

			final StringBuilder cmd = new StringBuilder();
			sq(cmd, exe);
			cmd.append(' ');
			sq(cmd, path);
			channel.setCommand(cmd.toString());
			channel.setErrStream(System.err);
			channel.connect();
			return channel;
		} catch (JSchException je) {
			throw new TransportException(uri.toString() + ": "
					+ je.getMessage(), je);
		}
	}

	class SshFetchConnection extends PackFetchConnection {
		private Session session;

		private ChannelExec channel;

		SshFetchConnection() throws TransportException {
			super(TransportGitSsh.this);
			try {
				session = openSession();
				channel = exec(session, getOptionUploadPack());
				init(channel.getInputStream(), channel.getOutputStream());
			} catch (TransportException err) {
				close();
				throw err;
			} catch (IOException err) {
				close();
				throw new TransportException(uri.toString()
						+ ": remote hung up unexpectedly", err);
			}
			readAdvertisedRefs();
		}

		@Override
		public void close() {
			super.close();

			if (channel != null) {
				try {
					if (channel.isConnected())
						channel.disconnect();
				} finally {
					channel = null;
				}
			}

			if (session != null) {
				try {
					sch.releaseSession(session);
				} finally {
					session = null;
				}
			}
		}
	}
}
