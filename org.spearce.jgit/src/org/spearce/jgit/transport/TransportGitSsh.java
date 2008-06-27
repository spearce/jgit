/*
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

	private static void sqMinimal(final StringBuilder cmd, final String val) {
		if (val.matches("^[a-zA-Z0-9._/-]*$")) {
			// If the string matches only generally safe characters
			// that the shell is not going to evaluate specially we
			// should leave the string unquoted. Not all systems
			// actually run a shell and over-quoting confuses them
			// when it comes to the command name.
			//
			cmd.append(val);
		} else {
			sq(cmd, val);
		}
	}

	private static void sqAlways(final StringBuilder cmd, final String val) {
		sq(cmd, val);
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
			sqMinimal(cmd, exe);
			cmd.append(' ');
			sqAlways(cmd, path);
			channel.setCommand(cmd.toString());
			channel.setErrStream(System.err);
			channel.connect();
			return channel;
		} catch (JSchException je) {
			throw new TransportException(uri.toString() + ": "
					+ je.getMessage(), je);
		}
	}

	class SshFetchConnection extends BasePackFetchConnection {
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
