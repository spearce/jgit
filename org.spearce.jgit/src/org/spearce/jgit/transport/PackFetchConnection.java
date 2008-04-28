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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.spearce.jgit.errors.PackProtocolException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;

/**
 * Fetch implementation using the native Git pack transfer service.
 * <p>
 * This is the canonical implementation for transferring objects from the remote
 * repository to the local repository by talking to the 'git-upload-pack'
 * service. Objects are packed on the remote side into a pack file and then sent
 * down the pipe to us.
 * <p>
 * This connection requires only a bi-directional pipe or socket, and thus is
 * easily wrapped up into a local process pipe, anonymous TCP socket, or a
 * command executed through an SSH tunnel.
 */
abstract class PackFetchConnection extends FetchConnection {
	/** The repository this transport fetches into, or pushes out of. */
	protected final Repository local;

	/** Capability tokens advertised by the remote side. */
	protected final Set<String> remoteCapablities = new HashSet<String>();

	/** Buffered input stream reading from the remote. */
	protected InputStream in;

	/** Buffered output stream sending to the remote. */
	protected OutputStream out;

	/** Packet line decoder around {@link #in}. */
	protected PacketLineIn pckIn;

	/** Packet line encoder around {@link #out}. */
	protected PacketLineOut pckOut;

	PackFetchConnection(final PackTransport packTransport) {
		local = packTransport.local;
	}

	protected void init(final InputStream myIn, final OutputStream myOut) {
		in = myIn instanceof BufferedInputStream ? myIn
				: new BufferedInputStream(myIn);
		out = myOut instanceof BufferedOutputStream ? myOut
				: new BufferedOutputStream(myOut);

		pckIn = new PacketLineIn(in);
		pckOut = new PacketLineOut(out);
	}

	protected void readAdvertisedRefs() throws TransportException {
		try {
			readAdvertisedRefsImpl();
		} catch (TransportException err) {
			close();
			throw err;
		} catch (IOException err) {
			close();
			throw new TransportException(err.getMessage(), err);
		} catch (RuntimeException err) {
			close();
			throw new TransportException(err.getMessage(), err);
		}
	}

	private void readAdvertisedRefsImpl() throws IOException {
		final LinkedHashMap<String, Ref> avail = new LinkedHashMap<String, Ref>();
		for (;;) {
			String line = pckIn.readString();

			if (avail.isEmpty()) {
				// The first line (if any) may contain "hidden"
				// capability values after a NUL byte.
				//
				final int nul = line.indexOf('\0');
				if (nul >= 0) {
					for (String c : line.substring(nul + 1).split(" "))
						remoteCapablities.add(c);
					line = line.substring(0, nul);
				}
			}

			if (line.length() == 0)
				break;

			String name = line.substring(41, line.length());
			final ObjectId id = ObjectId.fromString(line.substring(0, 40));
			if (name.endsWith("^{}")) {
				name = name.substring(0, name.length() - 3);
				final Ref prior = avail.get(name);
				if (prior == null)
					throw new PackProtocolException("advertisement of " + name
							+ "^{} came before " + name);

				if (prior.getPeeledObjectId() != null)
					throw duplicateAdvertisement(name + "^{}");

				avail.put(name, new Ref(name, prior.getObjectId(), id));
			} else {
				final Ref prior = avail.put(name, new Ref(name, id));
				if (prior != null)
					throw duplicateAdvertisement(name);
			}
		}
		available(avail);
	}

	private PackProtocolException duplicateAdvertisement(final String name) {
		return new PackProtocolException("duplicate advertisements of " + name);
	}

	@Override
	public void close() {
		if (out != null) {
			try {
				pckOut.end();
				out.close();
			} catch (IOException err) {
				// Ignore any close errors.
			} finally {
				out = null;
				pckOut = null;
			}
		}

		if (in != null) {
			try {
				in.close();
			} catch (IOException err) {
				// Ignore any close errors.
			} finally {
				in = null;
				pckIn = null;
			}
		}
	}
}