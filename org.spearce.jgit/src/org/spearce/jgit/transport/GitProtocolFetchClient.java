/*
 *  Copyright (C) 2008  Robin Rosenberg
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.spearce.jgit.lib.Repository;


/**
 * A {@link FetchClient} for local cloning using git protocol
 */
public class GitProtocolFetchClient extends FullFetchClient {

	/**
	 * IANA assigned port number for Git
	 */
	public static final int GIT_PROTO_PORT = 9418;

	private GitProtocolFetchClient(final Repository repository,
			final String remoteName, final String initialCommand,
			final OutputStream toServer, final InputStream fromServer)
			throws IOException {
		super(repository, remoteName, initialCommand, toServer, fromServer);
	}

	/**
	 * Create a FetchClient for local cloning using local git protocol
	 *
	 * @param repository
	 * @param remoteName
	 * @param host
	 * @param port IANA standard 9418
	 * @param remoteGitDir
	 * @return a {@link FetchClient} set up for cloning
	 * @throws IOException
	 */
	public static FetchClient create(final Repository repository,
			final String remoteName, final String host, final int port,
			final String remoteGitDir) throws IOException {
		final Socket socket = new Socket(InetAddress.getByName(host), port);
		final InputStream inputStream = socket.getInputStream();
		final OutputStream outpuStream = socket.getOutputStream();
		final String remoteCommand = "git-upload-pack "+remoteGitDir+"\0host="+host+"\0";
		return new GitProtocolFetchClient(repository, remoteName, remoteCommand, outpuStream, inputStream);
	}
}
