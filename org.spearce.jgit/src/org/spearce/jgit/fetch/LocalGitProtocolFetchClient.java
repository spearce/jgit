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
package org.spearce.jgit.fetch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.spearce.jgit.lib.Repository;


/**
 * A {@link FetchClient} for local cloning using git protocol.
 * <p>
 * The main use of this class is for testing the git protocol. Exception
 * for the initialization, i.e. an optional command to send at the very
 * beginning, the protocol is the same regardless of whether it is over
 * pipes, plain TCP sockets or an SSH connection.
 * <p>
 * <quote>Local</quote> can include network drives.
 */
public class LocalGitProtocolFetchClient extends FullFetchClient {

	private LocalGitProtocolFetchClient(final Repository repository,
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
	 * @param remoteGitDir
	 * @return a {@link FetchClient} set up for cloning
	 * @throws IOException
	 */
	public static FetchClient create(final Repository repository,
			final String remoteName, final File remoteGitDir)
			throws IOException {
		final Process process = Runtime.getRuntime().exec(
				new String[] { "git-upload-pack", "." }, null, remoteGitDir);
		final InputStream inputStream = process.getInputStream();
		final OutputStream outpuStream = process.getOutputStream();
		return new LocalGitProtocolFetchClient(repository, remoteName, null, outpuStream, inputStream);
	}
}
