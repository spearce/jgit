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

import org.spearce.jgit.lib.Repository;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * A {@link FetchClient} for local cloning using git protocol
 */
public class GitSshProtocolFetchClient extends FullFetchClient {

	/**
	 * Default port for Git over SSH, same as SSH default port
	 */
	public static final int GIT_SSH_PROTO_PORT = 22;

	private GitSshProtocolFetchClient(final Repository repository,
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
		final JSch sch = new JSch();
		final Session session = sch.getSession(username, host, port);
		final UserInfo userInfo = new UserInfo() {

			public void showMessage(String arg0) {
				System.out.println("Userinfo message:" + arg0);
			}

			public boolean promptYesNo(String arg0) {
				System.out.println("Userinfo promptYesNo:" + arg0);
				return true; // TODO - do not answer yes lightly
			}

			public boolean promptPassword(String arg0) {
				return true;
			}

			public boolean promptPassphrase(String arg0) {
				return false;
			}

			public String getPassword() {
				return password;
			}

			public String getPassphrase() {
				return null;
			}

		};
		session.setUserInfo(userInfo);
		session.connect();
		ChannelExec channel = (ChannelExec)session.openChannel("exec");
		channel.setCommand("git-upload-pack \""+remoteGitDir+"\"");
		final InputStream inputStream = channel.getInputStream();
		final OutputStream outpuStream = channel.getOutputStream();
		channel.setErrStream(System.err);
		channel.connect();

		return new GitSshProtocolFetchClient(repository, remoteName, null, outpuStream, inputStream);
	}
}
