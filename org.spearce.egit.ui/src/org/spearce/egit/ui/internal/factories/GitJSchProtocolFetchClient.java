/*
 *  Copyright (C) 2008  Robin Rosenberg
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
package org.spearce.egit.ui.internal.factories;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jsch.internal.ui.JSchUIPlugin;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.transport.FetchClient;
import org.spearce.jgit.transport.FullFetchClient;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * A {@link FetchClient} for local cloning using git protocol
 */
public class GitJSchProtocolFetchClient extends FullFetchClient {

	/**
	 * Default port for Git over SSH, same as SSH default port
	 */
	public static final int GIT_SSH_PROTO_PORT = 22;

	private GitJSchProtocolFetchClient(final Repository repository,
			final String remoteName, final String initialCommand,
			final OutputStream toServer, final InputStream fromServer)
			throws IOException {
		super(repository, remoteName, initialCommand, toServer, fromServer);
	}

	/**
	 * Create a FetchClient for cloning using git + ssh protocol
	 *
	 * @param repository
	 * @param remoteName
	 * @param host
	 * @param port standard is SSH port 22
	 * @param username
	 * @param password
	 * @param remoteGitDir
	 * @return a {@link FetchClient} set up for cloning
	 * @throws IOException
	 * @throws JSchException
	 */
	public static FetchClient create(final Repository repository,
			final String remoteName, final String host, final int port,
			final String username, final String password,
			final String remoteGitDir) throws IOException, JSchException {
		final Session session = JSchUIPlugin.getPlugin().getJSchService().createSession(host, port, username);
		if (password != null && password.length() > 0)
			session.setPassword(password);
		session.connect();
		ChannelExec channel = (ChannelExec)session.openChannel("exec");
		channel.setCommand("git-upload-pack \""+remoteGitDir+"\"");
		final InputStream inputStream = channel.getInputStream();
		final OutputStream outpuStream = channel.getOutputStream();
		channel.setErrStream(System.err);
		channel.connect();

		return new GitJSchProtocolFetchClient(repository, remoteName, null, outpuStream, inputStream);
	}
}
