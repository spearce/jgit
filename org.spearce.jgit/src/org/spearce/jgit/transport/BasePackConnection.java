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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.spearce.jgit.errors.NoRemoteRepositoryException;
import org.spearce.jgit.errors.PackProtocolException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;

/**
 * Base helper class for pack-based operations implementations. Provides partial
 * implementation of pack-protocol - refs advertising and capabilities support,
 * and some other helper methods.
 *
 * @see BasePackFetchConnection
 * @see BasePackPushConnection
 */
abstract class BasePackConnection extends BaseConnection {

	/** The repository this transport fetches into, or pushes out of. */
	protected final Repository local;

	/** Remote repository location. */
	protected final URIish uri;

	/** A transport connected to {@link #uri}. */
	protected final Transport transport;

	/** Buffered input stream reading from the remote. */
	protected InputStream in;

	/** Buffered output stream sending to the remote. */
	protected OutputStream out;

	/** Packet line decoder around {@link #in}. */
	protected PacketLineIn pckIn;

	/** Packet line encoder around {@link #out}. */
	protected PacketLineOut pckOut;

	/** Send {@link PacketLineOut#end()} before closing {@link #out}? */
	protected boolean outNeedsEnd;

	/** Capability tokens advertised by the remote side. */
	private final Set<String> remoteCapablities = new HashSet<String>();

	/** Extra objects the remote has, but which aren't offered as refs. */
	protected final Set<ObjectId> additionalHaves = new HashSet<ObjectId>();

	BasePackConnection(final PackTransport packTransport) {
		transport = (Transport)packTransport;
		local = transport.local;
		uri = transport.uri;
	}

	protected void init(final InputStream myIn, final OutputStream myOut) {
		in = myIn instanceof BufferedInputStream ? myIn
				: new BufferedInputStream(myIn, IndexPack.BUFFER_SIZE);
		out = myOut instanceof BufferedOutputStream ? myOut
				: new BufferedOutputStream(myOut);

		pckIn = new PacketLineIn(in);
		pckOut = new PacketLineOut(out);
		outNeedsEnd = true;
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
			String line;

			try {
				line = pckIn.readString();
			} catch (EOFException eof) {
				if (avail.isEmpty())
					throw noRepository();
				throw eof;
			}
			if (line == PacketLineIn.END)
				break;

			if (avail.isEmpty()) {
				final int nul = line.indexOf('\0');
				if (nul >= 0) {
					// The first line (if any) may contain "hidden"
					// capability values after a NUL byte.
					for (String c : line.substring(nul + 1).split(" "))
						remoteCapablities.add(c);
					line = line.substring(0, nul);
				}
			}

			String name = line.substring(41, line.length());
			if (avail.isEmpty() && name.equals("capabilities^{}")) {
				// special line from git-receive-pack to show
				// capabilities when there are no refs to advertise
				continue;
			}

			final ObjectId id = ObjectId.fromString(line.substring(0, 40));
			if (name.equals(".have")) {
				additionalHaves.add(id);
			} else if (name.endsWith("^{}")) {
				name = name.substring(0, name.length() - 3);
				final Ref prior = avail.get(name);
				if (prior == null)
					throw new PackProtocolException(uri, "advertisement of "
							+ name + "^{} came before " + name);

				if (prior.getPeeledObjectId() != null)
					throw duplicateAdvertisement(name + "^{}");

				avail.put(name, new Ref(Ref.Storage.NETWORK, name, prior
						.getObjectId(), id, true));
			} else {
				final Ref prior;
				prior = avail.put(name, new Ref(Ref.Storage.NETWORK, name, id));
				if (prior != null)
					throw duplicateAdvertisement(name);
			}
		}
		available(avail);
	}

	/**
	 * Create an exception to indicate problems finding a remote repository. The
	 * caller is expected to throw the returned exception.
	 *
	 * Subclasses may override this method to provide better diagnostics.
	 *
	 * @return a TransportException saying a repository cannot be found and
	 *         possibly why.
	 */
	protected TransportException noRepository() {
		return new NoRemoteRepositoryException(uri, "not found.");
	}

	protected boolean isCapableOf(final String option) {
		return remoteCapablities.contains(option);
	}

	protected boolean wantCapability(final StringBuilder b, final String option) {
		if (!isCapableOf(option))
			return false;
		b.append(' ');
		b.append(option);
		return true;
	}

	private PackProtocolException duplicateAdvertisement(final String name) {
		return new PackProtocolException(uri, "duplicate advertisements of "
				+ name);
	}

	@Override
	public void close() {
		if (out != null) {
			try {
				if (outNeedsEnd)
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
