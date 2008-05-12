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
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * Transport over the non-Git aware SFTP (SSH based FTP) protocol.
 * <p>
 * The SFTP transport does not require any specialized Git support on the remote
 * (server side) repository. Object files are retrieved directly through secure
 * shell's FTP protocol, making it possible to copy objects from a remote
 * repository that is available over SSH, but whose remote host does not have
 * Git installed.
 * <p>
 * Unlike the HTTP variant (see {@link TransportHttp}) we rely upon being able
 * to list files in directories, as the SFTP protocol supports this function. By
 * listing files through SFTP we can avoid needing to have current
 * <code>objects/info/packs</code> or <code>info/refs</code> files on the
 * remote repository and access the data directly, much as Git itself would.
 * 
 * @see WalkFetchConnection
 */
class TransportSftp extends WalkTransport {
	static boolean canHandle(final URIish uri) {
		return uri.isRemote() && "sftp".equals(uri.getScheme());
	}

	final SshSessionFactory sch;

	TransportSftp(final Repository local, final URIish uri) {
		super(local, uri);
		sch = SshSessionFactory.getInstance();
	}

	@Override
	public FetchConnection openFetch() throws TransportException {
		final SftpObjectDB c = new SftpObjectDB(uri.getPath());
		final WalkFetchConnection r = new WalkFetchConnection(this, c);
		c.readAdvertisedRefs(r);
		return r;
	}

	Session openSession() throws TransportException {
		final String user = uri.getUser();
		final String pass = uri.getPass();
		final String host = uri.getHost();
		final int port = uri.getPort();
		try {
			final Session session;
			session = sch.getSession(user, pass, host, port);
			if (!session.isConnected())
				session.connect();
			return session;
		} catch (JSchException je) {
			final String us = uri.toString();
			final Throwable c = je.getCause();
			if (c instanceof UnknownHostException)
				throw new TransportException(us + ": Unknown host");
			if (c instanceof ConnectException)
				throw new TransportException(us + ": " + c.getMessage());
			throw new TransportException(us + ": " + je.getMessage(), je);
		}
	}

	ChannelSftp open(final Session sock) throws TransportException {
		try {
			final Channel channel = sock.openChannel("sftp");
			channel.connect();
			return (ChannelSftp) channel;
		} catch (JSchException je) {
			throw new TransportException(uri.toString() + ": "
					+ je.getMessage(), je);
		}
	}

	class SftpObjectDB extends WalkRemoteObjectDatabase {
		private final String objectsPath;

		private final boolean sessionOwner;

		private Session session;

		private ChannelSftp ftp;

		SftpObjectDB(String path) throws TransportException {
			if (path.startsWith("/~"))
				path = path.substring(1);
			if (path.startsWith("~/"))
				path = path.substring(2);
			try {
				session = openSession();
				sessionOwner = true;
				ftp = TransportSftp.this.open(session);
				ftp.cd(path);
				ftp.cd("objects");
				objectsPath = ftp.pwd();
			} catch (TransportException err) {
				close();
				throw err;
			} catch (SftpException je) {
				throw new TransportException("Can't enter " + path + "/objects"
						+ ": " + je.getMessage(), je);
			}
		}

		SftpObjectDB(final SftpObjectDB parent, final String p)
				throws TransportException {
			sessionOwner = false;
			session = parent.session;
			try {
				ftp = TransportSftp.this.open(session);
				ftp.cd(parent.objectsPath);
				ftp.cd(p);
				objectsPath = ftp.pwd();
			} catch (TransportException err) {
				close();
				throw err;
			} catch (SftpException je) {
				throw new TransportException("Can't enter " + p + " from "
						+ parent.objectsPath + ": " + je.getMessage(), je);
			}
		}

		@Override
		Collection<WalkRemoteObjectDatabase> getAlternates() throws IOException {
			try {
				return readAlternates(INFO_ALTERNATES);
			} catch (FileNotFoundException err) {
				return null;
			}
		}

		@Override
		WalkRemoteObjectDatabase openAlternate(final String location)
				throws IOException {
			return new SftpObjectDB(this, location);
		}

		@Override
		Collection<String> getPackNames() throws IOException {
			final List<String> packs = new ArrayList<String>();
			try {
				final Collection<ChannelSftp.LsEntry> list = ftp.ls("pack");
				final HashMap<String, ChannelSftp.LsEntry> files;
				final HashMap<String, Integer> mtimes;

				files = new HashMap<String, ChannelSftp.LsEntry>();
				mtimes = new HashMap<String, Integer>();

				for (final ChannelSftp.LsEntry ent : list)
					files.put(ent.getFilename(), ent);
				for (final ChannelSftp.LsEntry ent : list) {
					final String n = ent.getFilename();
					if (!n.startsWith("pack-") || !n.endsWith(".pack"))
						continue;

					final String in = n.substring(0, n.length() - 5) + ".idx";
					if (!files.containsKey(in))
						continue;

					mtimes.put(n, ent.getAttrs().getMTime());
					packs.add(n);
				}

				Collections.sort(packs, new Comparator<String>() {
					public int compare(final String o1, final String o2) {
						return mtimes.get(o2) - mtimes.get(o1);
					}
				});
			} catch (SftpException je) {
				throw new TransportException("Can't ls " + objectsPath
						+ "/pack: " + je.getMessage(), je);
			}
			return packs;
		}

		@Override
		FileStream open(final String path) throws IOException {
			try {
				final SftpATTRS a = ftp.lstat(path);
				return new FileStream(ftp.get(path), a.getSize());
			} catch (SftpException je) {
				if (je.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
					throw new FileNotFoundException(path);
				throw new TransportException("Can't get " + objectsPath + "/"
						+ path + ": " + je.getMessage(), je);
			}
		}

		void readAdvertisedRefs(final WalkFetchConnection connection)
				throws TransportException {
			final TreeMap<String, Ref> avail = new TreeMap<String, Ref>();
			try {
				final BufferedReader br = openReader("../packed-refs");
				try {
					readPackedRefs(avail, br);
				} finally {
					br.close();
				}
			} catch (FileNotFoundException notPacked) {
				// Perhaps it wasn't worthwhile, or is just an older repository.
			} catch (IOException e) {
				throw new TransportException(uri + ": error in packed-refs", e);
			}
			readRef(avail, "../HEAD", "HEAD");
			readLooseRefs(avail, "../refs", "refs/");
			connection.available(avail);
		}

		private void readPackedRefs(final TreeMap<String, Ref> avail,
				final BufferedReader br) throws IOException {
			Ref last = null;
			for (;;) {
				String line = br.readLine();
				if (line == null)
					break;
				if (line.charAt(0) == '#')
					continue;
				if (line.charAt(0) == '^') {
					if (last == null)
						throw new TransportException("Peeled line before ref.");
					final ObjectId id = ObjectId.fromString(line + 1);
					last = new Ref(last.getName(), last.getObjectId(), id);
					avail.put(last.getName(), last);
					continue;
				}

				final int sp = line.indexOf(' ');
				if (sp < 0)
					throw new TransportException("Unrecognized ref: " + line);
				final ObjectId id = ObjectId.fromString(line.substring(0, sp));
				final String name = line.substring(sp + 1);
				last = new Ref(name, id);
				avail.put(last.getName(), last);
			}
		}

		private void readLooseRefs(final TreeMap<String, Ref> avail,
				final String dir, final String prefix)
				throws TransportException {
			final Collection<ChannelSftp.LsEntry> list;
			try {
				list = ftp.ls(dir);
			} catch (SftpException je) {
				throw new TransportException("Can't ls " + objectsPath + "/"
						+ dir + ": " + je.getMessage(), je);
			}

			for (final ChannelSftp.LsEntry ent : list) {
				final String n = ent.getFilename();
				if (".".equals(n) || "..".equals(n))
					continue;

				final String nPath = dir + "/" + n;
				if (ent.getAttrs().isDir())
					readLooseRefs(avail, nPath, prefix + n + "/");
				else
					readRef(avail, nPath, prefix + n);
			}
		}

		private Ref readRef(final TreeMap<String, Ref> avail,
				final String path, final String name) throws TransportException {
			final String line;
			try {
				final BufferedReader br = openReader(path);
				try {
					line = br.readLine();
				} finally {
					br.close();
				}
			} catch (FileNotFoundException noRef) {
				return null;
			} catch (IOException err) {
				throw new TransportException("Cannot read " + objectsPath + "/"
						+ path + ": " + err.getMessage(), err);
			}

			if (line == null)
				throw new TransportException("Empty ref: " + name);

			if (line.startsWith("ref: ")) {
				final String p = line.substring("ref: ".length());
				Ref r = readRef(avail, "../" + p, p);
				if (r == null)
					r = avail.get(p);
				if (r != null) {
					r = new Ref(name, r.getObjectId(), r.getPeeledObjectId());
					avail.put(name, r);
				}
				return r;
			}

			if (ObjectId.isId(line)) {
				final Ref r = new Ref(name, ObjectId.fromString(line));
				avail.put(r.getName(), r);
				return r;
			}

			throw new TransportException("Bad ref: " + name + ": " + line);
		}

		@Override
		void close() {
			if (ftp != null) {
				try {
					if (ftp.isConnected())
						ftp.disconnect();
				} finally {
					ftp = null;
				}
			}

			if (sessionOwner && session != null) {
				try {
					sch.releaseSession(session);
				} finally {
					session = null;
				}
			}
		}
	}
}
