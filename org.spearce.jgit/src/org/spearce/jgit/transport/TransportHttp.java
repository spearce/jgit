/*
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.PackProtocolException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;

/**
 * Transport over the non-Git aware HTTP and FTP protocol.
 * <p>
 * The HTTP transport does not require any specialized Git support on the remote
 * (server side) repository. Object files are retrieved directly through
 * standard HTTP GET requests, making it easy to serve a Git repository through
 * a standard web host provider that does not offer specific support for Git.
 * 
 * @see WalkFetchConnection
 */
class TransportHttp extends WalkTransport {
	static boolean canHandle(final URIish uri) {
		if (!uri.isRemote())
			return false;
		final String s = uri.getScheme();
		return "http".equals(s) || "https".equals(s) || "ftp".equals(s);
	}

	private final URL baseUrl;

	private final URL objectsUrl;

	private final ProxySelector proxySelector;

	TransportHttp(final Repository local, final URIish uri)
			throws NotSupportedException {
		super(local, uri);
		try {
			String uriString = uri.toString();
			if (!uriString.endsWith("/"))
				uriString += "/";
			baseUrl = new URL(uriString);
			objectsUrl = new URL(baseUrl, "objects/");
		} catch (MalformedURLException e) {
			throw new NotSupportedException("Invalid URL " + uri, e);
		}
		proxySelector = ProxySelector.getDefault();
	}

	@Override
	public FetchConnection openFetch() throws TransportException {
		final HttpObjectDB c = new HttpObjectDB(objectsUrl);
		final WalkFetchConnection r = new WalkFetchConnection(this, c);
		r.available(c.readAdvertisedRefs());
		return r;
	}

	Proxy proxyFor(final URL u) throws URISyntaxException {
		return proxySelector.select(u.toURI()).get(0);
	}

	class HttpObjectDB extends WalkRemoteObjectDatabase {
		private static final String INFO_REFS = "../info/refs";

		private final URL objectsUrl;

		HttpObjectDB(final URL b) {
			objectsUrl = b;
		}

		@Override
		Collection<WalkRemoteObjectDatabase> getAlternates() throws IOException {
			try {
				return readAlternates(INFO_HTTP_ALTERNATES);
			} catch (FileNotFoundException err) {
				// Fall through.
			}

			try {
				return readAlternates(INFO_ALTERNATES);
			} catch (FileNotFoundException err) {
				// Fall through.
			}

			return null;
		}

		@Override
		WalkRemoteObjectDatabase openAlternate(final String location)
				throws IOException {
			return new HttpObjectDB(new URL(objectsUrl, location));
		}

		@Override
		Collection<String> getPackNames() throws IOException {
			final Collection<String> packs = new ArrayList<String>();
			try {
				final BufferedReader br = openReader(INFO_PACKS);
				try {
					for (;;) {
						final String s = br.readLine();
						if (s == null || s.length() == 0)
							break;
						if (!s.startsWith("P pack-") || !s.endsWith(".pack"))
							throw invalidAdvertisement(s);
						packs.add(s.substring(2));
					}
					return packs;
				} finally {
					br.close();
				}
			} catch (FileNotFoundException err) {
				return packs;
			}
		}

		@Override
		FileStream open(final String path) throws IOException {
			final URL base = objectsUrl;
			try {
				final URL u = new URL(base, path);
				final URLConnection c = u.openConnection(proxyFor(u));
				final InputStream in = c.getInputStream();
				final int len = c.getContentLength();
				return new FileStream(in, len);
			} catch (ConnectException ce) {
				// The standard J2SE error message is not very useful.
				//
				if ("Connection timed out: connect".equals(ce.getMessage()))
					throw new ConnectException("Connection timed out: " + base);
				throw new ConnectException(ce.getMessage() + " " + base);
			} catch (URISyntaxException e) {
				final ConnectException err;
				err = new ConnectException("Cannot determine proxy for " + base);
				err.initCause(e);
				throw err;
			}
		}

		Map<String, Ref> readAdvertisedRefs() throws TransportException {
			try {
				final BufferedReader br = openReader(INFO_REFS);
				try {
					return readAdvertisedImpl(br);
				} finally {
					br.close();
				}
			} catch (IOException err) {
				try {
					throw new TransportException(new URL(objectsUrl, INFO_REFS)
							+ ": cannot read available refs", err);
				} catch (MalformedURLException mue) {
					throw new TransportException(objectsUrl + INFO_REFS
							+ ": cannot read available refs", err);
				}
			}
		}

		private Map<String, Ref> readAdvertisedImpl(final BufferedReader br)
				throws IOException, PackProtocolException {
			final TreeMap<String, Ref> avail = new TreeMap<String, Ref>();
			for (;;) {
				String line = br.readLine();
				if (line == null)
					break;

				final int tab = line.indexOf('\t');
				if (tab < 0)
					throw invalidAdvertisement(line);

				String name;
				final ObjectId id;

				name = line.substring(tab + 1);
				id = ObjectId.fromString(line.substring(0, tab));
				if (name.endsWith("^{}")) {
					name = name.substring(0, name.length() - 3);
					final Ref prior = avail.get(name);
					if (prior == null)
						throw outOfOrderAdvertisement(name);

					if (prior.getPeeledObjectId() != null)
						throw duplicateAdvertisement(name + "^{}");

					avail.put(name, new Ref(Ref.Storage.NETWORK, name, prior
							.getObjectId(), id));
				} else {
					final Ref prior = avail.put(name, new Ref(
							Ref.Storage.NETWORK, name, id));
					if (prior != null)
						throw duplicateAdvertisement(name);
				}
			}
			return avail;
		}

		private PackProtocolException outOfOrderAdvertisement(final String n) {
			return new PackProtocolException("advertisement of " + n
					+ "^{} came before " + n);
		}

		private PackProtocolException invalidAdvertisement(final String n) {
			return new PackProtocolException("invalid advertisement of " + n);
		}

		private PackProtocolException duplicateAdvertisement(final String n) {
			return new PackProtocolException("duplicate advertisements of " + n);
		}

		@Override
		void close() {
			// We do not maintain persistent connections.
		}
	}
}
