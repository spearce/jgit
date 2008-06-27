/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.errors.NotSupportedException;
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

	@Override
	public PushConnection openPush() throws NotSupportedException,
			TransportException {
		return new LocalPushConnection();
	}

	protected Process startProcessWithErrStream(final String cmd)
			throws TransportException {
		try {
			final Process proc = Runtime.getRuntime().exec(
					new String[] { cmd, "." }, null, remoteGitDir);
			new StreamRewritingThread(proc.getErrorStream()).start();
			return proc;
		} catch (IOException err) {
			throw new TransportException(uri.toString() + ": "
					+ err.getMessage(), err);
		}
	}

	class LocalFetchConnection extends BasePackFetchConnection {
		private Process uploadPack;

		LocalFetchConnection() throws TransportException {
			super(TransportLocal.this);
			uploadPack = startProcessWithErrStream(getOptionReceivePack());
			init(uploadPack.getInputStream(), uploadPack.getOutputStream());
			readAdvertisedRefs();
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

	class LocalPushConnection extends BasePackPushConnection {
		private Process receivePack;

		LocalPushConnection() throws TransportException {
			super(TransportLocal.this);
			receivePack = startProcessWithErrStream(getOptionReceivePack());
			init(receivePack.getInputStream(), receivePack.getOutputStream());
			readAdvertisedRefs();
		}

		@Override
		public void close() {
			super.close();

			if (receivePack != null) {
				try {
					receivePack.waitFor();
				} catch (InterruptedException ie) {
					// Stop waiting and return anyway.
				} finally {
					receivePack = null;
				}
			}
		}
	}

	class StreamRewritingThread extends Thread {
		private final InputStream in;

		StreamRewritingThread(final InputStream in) {
			super("JGit " + getOptionUploadPack() + " Errors");
			this.in = in;
		}

		public void run() {
			final byte[] tmp = new byte[512];
			try {
				for (;;) {
					final int n = in.read(tmp);
					if (n < 0)
						break;
					System.err.write(tmp, 0, n);
					System.err.flush();
				}
			} catch (IOException err) {
				// Ignore errors reading errors.
			} finally {
				try {
					in.close();
				} catch (IOException err2) {
					// Ignore errors closing the pipe.
				}
			}
		}
	}
}
