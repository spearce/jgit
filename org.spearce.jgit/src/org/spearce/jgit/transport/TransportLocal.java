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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.util.FS;

/**
 * Transport that executes the Git "remote side" processes on a local directory.
 * <p>
 * This transport is suitable for use on the local system, where the caller has
 * direct read or write access to the remote repository. This implementation
 * forks a C Git process to provide the remote side access, much as the
 * {@link TransportGitSsh} implementation causes the remote side to run a C Git
 * process.
 */
class TransportLocal extends PackTransport {
	static boolean canHandle(final URIish uri) {
		if (uri.getHost() != null || uri.getPort() > 0 || uri.getUser() != null
				|| uri.getPass() != null || uri.getPath() == null)
			return false;

		if ("file".equals(uri.getScheme()) || uri.getScheme() == null)
			return FS.resolve(new File("."), uri.getPath()).isDirectory();
		return false;
	}

	private final File remoteGitDir;

	TransportLocal(final Repository local, final URIish uri) {
		super(local, uri);

		File d = FS.resolve(new File("."), uri.getPath()).getAbsoluteFile();
		if (new File(d, ".git").isDirectory())
			d = new File(d, ".git");
		remoteGitDir = d;
	}

	@Override
	public FetchConnection openFetch() throws TransportException {
		return new LocalFetchConnection();
	}

	class LocalFetchConnection extends PackFetchConnection {
		private Process uploadPack;

		LocalFetchConnection() throws TransportException {
			super(TransportLocal.this);
			try {
				uploadPack = Runtime.getRuntime().exec(
						new String[] { getOptionUploadPack(), "." }, null,
						remoteGitDir);
			} catch (IOException err) {
				throw new TransportException(uri.toString() + ": "
						+ err.getMessage(), err);
			}
			startErrorThread();
			init(uploadPack.getInputStream(), uploadPack.getOutputStream());
			readAdvertisedRefs();
		}

		private void startErrorThread() {
			final InputStream errorStream = uploadPack.getErrorStream();
			new Thread("JGit " + getOptionUploadPack() + " Errors") {
				public void run() {
					final byte[] tmp = new byte[512];
					try {
						for (;;) {
							final int n = errorStream.read(tmp);
							if (n < 0)
								break;
							System.err.write(tmp, 0, n);
							System.err.flush();
						}
					} catch (IOException err) {
						// Ignore errors reading errors.
					} finally {
						try {
							errorStream.close();
						} catch (IOException err2) {
							// Ignore errors closing the pipe.
						}
					}
				}
			}.start();
		}

		@Override
		public void close() {
			super.close();

			if (uploadPack != null) {
				try {
					uploadPack.waitFor();
				} catch (InterruptedException ie) {
					// Stop waiting and return anyway.
				} finally {
					uploadPack = null;
				}
			}
		}
	}
}
