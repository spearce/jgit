/*
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
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
import java.io.OutputStream;

import org.spearce.jgit.errors.NoRemoteRepositoryException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.util.QuotedString;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;

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
public class TransportGitSsh extends SshTransport implements PackTransport {
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

	OutputStream errStream;

	TransportGitSsh(final Repository local, final URIish uri) {
		super(local, uri);
	}

	@Override
	public FetchConnection openFetch() throws TransportException {
		return new SshFetchConnection();
	}

	@Override
	public PushConnection openPush() throws TransportException {
		return new SshPushConnection();
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
		if (val.length() > 0)
			cmd.append(QuotedString.BOURNE.quote(val));
	}


	ChannelExec exec(final String exe) throws TransportException {
		initSession();

		try {
			final ChannelExec channel = (ChannelExec) sock.openChannel("exec");
			String path = uri.getPath();
			if (uri.getScheme() != null && uri.getPath().startsWith("/~"))
				path = (uri.getPath().substring(1));

			final StringBuilder cmd = new StringBuilder();
			final int gitspace = exe.indexOf("git ");
			if (gitspace >= 0) {
				sqMinimal(cmd, exe.substring(0, gitspace + 3));
				cmd.append(' ');
				sqMinimal(cmd, exe.substring(gitspace + 4));
			} else
				sqMinimal(cmd, exe);
			cmd.append(' ');
			sqAlways(cmd, path);
			channel.setCommand(cmd.toString());
			errStream = createErrorStream();
			channel.setErrStream(errStream, true);
			channel.connect();
			return channel;
		} catch (JSchException je) {
			throw new TransportException(uri, je.getMessage(), je);
		}
	}

	/**
	 * @return the error stream for the channel, the stream is used to detect
	 *         specific error reasons for exceptions.
	 */
	private static OutputStream createErrorStream() {
		return new OutputStream() {
			private StringBuilder all = new StringBuilder();

			private StringBuilder sb = new StringBuilder();

			public String toString() {
				String r = all.toString();
				while (r.endsWith("\n"))
					r = r.substring(0, r.length() - 1);
				return r;
			}

			@Override
			public void write(final int b) throws IOException {
				if (b == '\r') {
					System.err.print('\r');
					return;
				}

				sb.append((char) b);

				if (b == '\n') {
					final String line = sb.toString();
					System.err.print(line);
					all.append(line);
					sb = new StringBuilder();
				}
			}
		};
	}

	NoRemoteRepositoryException cleanNotFound(NoRemoteRepositoryException nf) {
		String why = errStream.toString();
		if (why == null || why.length() == 0)
			return nf;

		String path = uri.getPath();
		if (uri.getScheme() != null && uri.getPath().startsWith("/~"))
			path = uri.getPath().substring(1);

		final StringBuilder pfx = new StringBuilder();
		pfx.append("fatal: ");
		sqAlways(pfx, path);
		pfx.append(": ");
		if (why.startsWith(pfx.toString()))
			why = why.substring(pfx.length());

		return new NoRemoteRepositoryException(uri, why);
	}

	class SshFetchConnection extends BasePackFetchConnection {
		private ChannelExec channel;

		SshFetchConnection() throws TransportException {
			super(TransportGitSsh.this);
			try {
				channel = exec(getOptionUploadPack());

				if (channel.isConnected())
					init(channel.getInputStream(), channel.getOutputStream());
				else
					throw new TransportException(uri, errStream.toString());

			} catch (TransportException err) {
				close();
				throw err;
			} catch (IOException err) {
				close();
				throw new TransportException(uri,
						"remote hung up unexpectedly", err);
			}

			try {
				readAdvertisedRefs();
			} catch (NoRemoteRepositoryException notFound) {
				throw cleanNotFound(notFound);
			}
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
		}
	}

	class SshPushConnection extends BasePackPushConnection {
		private ChannelExec channel;

		SshPushConnection() throws TransportException {
			super(TransportGitSsh.this);
			try {
				channel = exec(getOptionReceivePack());

				if (channel.isConnected())
					init(channel.getInputStream(), channel.getOutputStream());
				else
					throw new TransportException(uri, errStream.toString());

			} catch (TransportException err) {
				close();
				throw err;
			} catch (IOException err) {
				close();
				throw new TransportException(uri,
						"remote hung up unexpectedly", err);
			}

			try {
				readAdvertisedRefs();
			} catch (NoRemoteRepositoryException notFound) {
				throw cleanNotFound(notFound);
			}
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
		}
	}
}
