/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui;

import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.ui.UserInfoPrompter;
import org.spearce.jgit.transport.SshConfigSessionFactory;
import org.spearce.jgit.transport.OpenSshConfig.Host;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

class EclipseSshSessionFactory extends SshConfigSessionFactory {
	private final IJSchService provider;

	EclipseSshSessionFactory(final IJSchService p) {
		provider = p;
	}

	@Override
	protected Session createSession(final String user, final String host,
			final int port) throws JSchException {
		return provider.createSession(host, port, user);
	}

	@Override
	protected void configure(final Host hc, final Session session) {
		if (!hc.isBatchMode())
			new UserInfoPrompter(session);
	}

	@Override
	protected JSch getUserJSch() throws JSchException {
		return provider.getJSch();
	}
}
