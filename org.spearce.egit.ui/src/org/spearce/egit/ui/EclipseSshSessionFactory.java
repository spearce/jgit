/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.ui.UserInfoPrompter;
import org.spearce.jgit.transport.OpenSshConfig;
import org.spearce.jgit.transport.SshSessionFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

class EclipseSshSessionFactory extends SshSessionFactory {
	private final IJSchService provider;

	private final Set<String> loadedIdentities = new HashSet<String>();

	private OpenSshConfig config;

	EclipseSshSessionFactory(final IJSchService p) {
		provider = p;
	}

	@Override
	public Session getSession(String user, String pass, String host, int port)
			throws JSchException {
		final OpenSshConfig.Host hc = getConfig().lookup(host);
		host = hc.getHostName();
		if (port <= 0)
			port = hc.getPort();
		if (user == null)
			user = hc.getUser();

		final Session session = provider.createSession(host, port, user);
		if (hc.getIdentityFile() != null)
			addIdentity(hc.getIdentityFile());
		if (pass != null)
			session.setPassword(pass);
		else if (!hc.isBatchMode())
			new UserInfoPrompter(session);

		final String pauth = hc.getPreferredAuthentications();
		if (pauth != null)
			session.setConfig("PreferredAuthentications", pauth);
		return session;
	}

	private synchronized OpenSshConfig getConfig() {
		if (config == null)
			config = OpenSshConfig.get();
		return config;
	}

	private void addIdentity(final File identityFile)
			throws JSchException {
		final String path = identityFile.getAbsolutePath();
		if (loadedIdentities.add(path))
			provider.getJSch().addIdentity(path);
	}

	@Override
	public OutputStream getErrorStream() {
		return new OutputStream() {

			StringBuilder all = new StringBuilder();

			StringBuilder sb = new StringBuilder();

			public String toString() {
				String r = all.toString();
				while (r.endsWith("\n"))
					r = r.substring(0, r.length() - 1);
				return r;
			}

			@Override
			public void write(int b) throws IOException {
				if (b == '\r')
					return;
				sb.append((char) b);
				if (b == '\n') {
					String s = sb.toString();
					all.append(s);
					sb = new StringBuilder();
					Activator.logError(s, new Throwable());
				}
			}
		};
	}

}
