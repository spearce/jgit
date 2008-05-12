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
class TransportGitAnon extends PackTransport {
	/** IANA assigned port number for Git. */
	static final int GIT_PORT = 9418;

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

	Socket openConnection() throws TransportException {
		final int port = uri.getPort() > 0 ? uri.getPort() : GIT_PORT;
		try {
			return new Socket(InetAddress.getByName(uri.getHost()), port);
		} catch (IOException c) {
			final String us = uri.toString();
			if (c instanceof UnknownHostException)
				throw new TransportException(us + ": Unknown host");
			if (c instanceof ConnectException)
				throw new TransportException(us + ": " + c.getMessage());
			throw new TransportException(us + ": " + c.getMessage(), c);
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
		cmd.append('\0');
		pckOut.writeString(cmd.toString());
		pckOut.flush();
	}

	class TcpFetchConnection extends PackFetchConnection {
		private Socket sock;

		TcpFetchConnection() throws TransportException {
			super(TransportGitAnon.this);
			sock = openConnection();
			try {
				init(sock.getInputStream(), sock.getOutputStream());
				service("git-upload-pack", pckOut);
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
