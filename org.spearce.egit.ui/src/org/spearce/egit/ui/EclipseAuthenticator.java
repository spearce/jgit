/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

class EclipseAuthenticator extends Authenticator {
	private final IProxyService service;

	EclipseAuthenticator(final IProxyService s) {
		service = s;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		final IProxyData[] data = service.getProxyData();
		if (data == null)
			return null;
		for (final IProxyData d : data) {
			if (d.getUserId() == null || d.getHost() == null)
				continue;
			if (d.getPort() == getRequestingPort() && hostMatches(d))
				return auth(d);
		}
		return null;
	}

	private PasswordAuthentication auth(final IProxyData d) {
		final String user = d.getUserId();
		final String pass = d.getPassword();
		final char[] passChar = pass != null ? pass.toCharArray() : new char[0];
		return new PasswordAuthentication(user, passChar);
	}

	private boolean hostMatches(final IProxyData d) {
		try {
			final InetAddress dHost = InetAddress.getByName(d.getHost());
			InetAddress rHost = getRequestingSite();
			if (rHost == null)
				rHost = InetAddress.getByName(getRequestingHost());
			return dHost.equals(rHost);
		} catch (UnknownHostException err) {
			return false;
		}
	}
}
