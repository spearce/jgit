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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Repository;

/**
 * Transport through a git-daemon waiting for anonymous TCP connections.
 * <p>
 * This transport supports the <code>git://</code> protocol, usually run on
 * the IANA registered port 9418. It is a popular means for distributing open
 * source projects, as there are no authentication or authorization overheads.
 */
class TransportGitAnon extends TcpTransport implements PackTransport {
	static final int GIT_PORT = Daemon.DEFAULT_PORT;

	static boolean canHandle(final URIish uri) {
		return "git".equals(uri.getScheme());
	}

	TransportGitAnon(final Repository local, final URIish uri) {
		super(local, uri);
	}

	@Override
	public FetchConnection openFetch() throws TransportException {
		return new TcpFetchConnection();
	}

	@Override
	public PushConnection openPush() throws TransportException {
		return new TcpPushConnection();
	}

	@Override
	public void close() {
		// Resources must be established per-connection.
	}

	Socket openConnection() throws TransportException {
		final int port = uri.getPort() > 0 ? uri.getPort() : GIT_PORT;
		try {
			return new Socket(InetAddress.getByName(uri.getHost()), port);
		} catch (IOException c) {
			if (c instanceof UnknownHostException)
				throw new TransportException(uri, "unknown host");
			if (c instanceof ConnectException)
				throw new TransportException(uri, c.getMessage());
			throw new TransportException(uri, c.getMessage(), c);
		}
	}

	void service(final String name, final PacketLineOut pckOut)
			throws IOException {
		final StringBuilder cmd = new StringBuilder();
		cmd.append(name);
		cmd.append(' ');
		cmd.append(uri.getPath());
		cmd.append('\0');
		cmd.append("host=");
		cmd.append(uri.getHost());
		if (uri.getPort() > 0 && uri.getPort() != GIT_PORT) {
			cmd.append(":");
			cmd.append(uri.getPort());
		}
		cmd.append('\0');
		pckOut.writeString(cmd.toString());
		pckOut.flush();
	}

	class TcpFetchConnection extends BasePackFetchConnection {
		private Socket sock;

		TcpFetchConnection() throws TransportException {
			super(TransportGitAnon.this);
			sock = openConnection();
			try {
				init(sock.getInputStream(), sock.getOutputStream());
				service("git-upload-pack", pckOut);
			} catch (IOException err) {
				close();
				throw new TransportException(uri,
						"remote hung up unexpectedly", err);
			}
			readAdvertisedRefs();
		}

		@Override
		public void close() {
			super.close();

			if (sock != null) {
				try {
					sock.close();
				} catch (IOException err) {
					// Ignore errors during close.
				} finally {
					sock = null;
				}
			}
		}
	}

	class TcpPushConnection extends BasePackPushConnection {
		private Socket sock;

		TcpPushConnection() throws TransportException {
			super(TransportGitAnon.this);
			sock = openConnection();
			try {
				init(sock.getInputStream(), sock.getOutputStream());
				service("git-receive-pack", pckOut);
			} catch (IOException err) {
				close();
				throw new TransportException(uri,
						"remote hung up unexpectedly", err);
			}
			readAdvertisedRefs();
		}

		@Override
		public void close() {
			super.close();

			if (sock != null) {
				try {
					sock.close();
				} catch (IOException err) {
					// Ignore errors during close.
				} finally {
					sock = null;
				}
			}
		}
	}
}
