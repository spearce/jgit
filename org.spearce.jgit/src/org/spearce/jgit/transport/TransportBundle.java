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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.PackProtocolException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.util.FS;

/**
 * Supports fetching from a git bundle (sneaker-net object transport).
 * <p>
 * Push support for a bundle is complex, as one does not have a peer to
 * communicate with to decide what the peer already knows. So push is not
 * supported by the bundle transport.
 */
class TransportBundle extends PackTransport {
	static final String V2_BUNDLE_SIGNATURE = "# v2 git bundle";

	static boolean canHandle(final URIish uri) {
		if (uri.getHost() != null || uri.getPort() > 0 || uri.getUser() != null
				|| uri.getPass() != null || uri.getPath() == null)
			return false;

		if ("file".equals(uri.getScheme()) || uri.getScheme() == null) {
			final File f = FS.resolve(new File("."), uri.getPath());
			return f.isFile() || f.getName().endsWith(".bundle");
		}

		return false;
	}

	private final File bundle;

	TransportBundle(final Repository local, final URIish uri) {
		super(local, uri);
		bundle = FS.resolve(new File("."), uri.getPath()).getAbsoluteFile();
	}

	@Override
	public FetchConnection openFetch() throws NotSupportedException,
			TransportException {
		return new BundleFetchConnection();
	}

	class BundleFetchConnection extends FetchConnection {
		FileInputStream in;

		RewindBufferedInputStream bin;

		final Set<ObjectId> prereqs = new HashSet<ObjectId>();

		BundleFetchConnection() throws TransportException {
			try {
				in = new FileInputStream(bundle);
				bin = new RewindBufferedInputStream(in);
			} catch (FileNotFoundException err) {
				throw new TransportException(bundle.getPath() + ": not found");
			}

			try {
				switch (readSignature()) {
				case 2:
					readBundleV2();
					break;
				default:
					throw new TransportException(bundle.getPath()
							+ ": not a bundle");
				}

				in.getChannel().position(
						in.getChannel().position() - bin.buffered());
				bin = null;
			} catch (TransportException err) {
				close();
				throw err;
			} catch (IOException err) {
				close();
				throw new TransportException(bundle.getPath() + ": "
						+ err.getMessage(), err);
			} catch (RuntimeException err) {
				close();
				throw new TransportException(bundle.getPath() + ": "
						+ err.getMessage(), err);
			}
		}

		private int readSignature() throws IOException {
			final String rev = readLine(new byte[1024]);
			if (V2_BUNDLE_SIGNATURE.equals(rev))
				return 2;
			throw new TransportException(bundle.getPath() + ": not a bundle");
		}

		private void readBundleV2() throws IOException {
			final byte[] hdrbuf = new byte[1024];
			final LinkedHashMap<String, Ref> avail = new LinkedHashMap<String, Ref>();
			for (;;) {
				String line = readLine(hdrbuf);
				if (line.length() == 0)
					break;

				if (line.charAt(0) == '-') {
					prereqs.add(ObjectId.fromString(line.substring(1, 41)));
					continue;
				}

				final String name = line.substring(41, line.length());
				final ObjectId id = ObjectId.fromString(line.substring(0, 40));
				final Ref prior = avail.put(name, new Ref(name, id));
				if (prior != null)
					throw duplicateAdvertisement(name);
			}
			available(avail);
		}

		private PackProtocolException duplicateAdvertisement(final String name) {
			return new PackProtocolException("duplicate advertisements of "
					+ name);
		}

		private String readLine(final byte[] hdrbuf) throws IOException {
			bin.mark(hdrbuf.length);
			final int cnt = bin.read(hdrbuf);
			int lf = 0;
			while (lf < cnt && hdrbuf[lf] != '\n')
				lf++;
			bin.reset();
			bin.skip(lf);
			if (lf < cnt && hdrbuf[lf] == '\n')
				bin.skip(1);
			return new String(hdrbuf, 0, lf, Constants.CHARACTER_ENCODING);
		}

		@Override
		protected void doFetch(final ProgressMonitor monitor,
				final Collection<Ref> want) throws TransportException {
			try {
				final IndexPack ip = IndexPack.create(local, in);
				ip.setFixThin(true);
				ip.index(monitor);
				ip.renameAndOpenPack();
			} catch (IOException err) {
				close();
				throw new TransportException(err.getMessage(), err);
			} catch (RuntimeException err) {
				close();
				throw new TransportException(err.getMessage(), err);
			}
		}

		@Override
		public void close() {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ie) {
					// Ignore close failures.
				} finally {
					in = null;
					bin = null;
				}
			}
		}

		class RewindBufferedInputStream extends BufferedInputStream {
			RewindBufferedInputStream(final InputStream src) {
				super(src);
			}

			int buffered() {
				return (count - pos);
			}
		}
	}
}
