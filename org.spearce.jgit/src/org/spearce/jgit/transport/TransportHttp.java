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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
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
	}

	@Override
	public FetchConnection openFetch() throws TransportException {
		final HttpObjectDB c = new HttpObjectDB(objectsUrl);
		final WalkFetchConnection r = new WalkFetchConnection(this, c);
		c.readAdvertisedRefs(r);
		return r;
	}

	static class HttpObjectDB extends WalkRemoteObjectDatabase {
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
			final URL u = new URL(objectsUrl, path);
			final URLConnection c = u.openConnection();
			final InputStream in = c.getInputStream();
			final int len = c.getContentLength();
			return new FileStream(in, len);
		}

		void readAdvertisedRefs(final WalkFetchConnection c)
				throws TransportException {
			try {
				final BufferedReader br = openReader(INFO_REFS);
				try {
					readAdvertisedImpl(br, c);
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

		private void readAdvertisedImpl(final BufferedReader br,
				final WalkFetchConnection connection) throws IOException,
				PackProtocolException {
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

					avail.put(name, new Ref(name, prior.getObjectId(), id));
				} else {
					final Ref prior = avail.put(name, new Ref(name, id));
					if (prior != null)
						throw duplicateAdvertisement(name);
				}
			}
			connection.available(avail);
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
