/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.ui;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

class EclipseProxySelector extends ProxySelector {
	private final IProxyService service;

	EclipseProxySelector(final IProxyService s) {
		service = s;
	}

	@Override
	public List<Proxy> select(final URI uri) {
		final ArrayList<Proxy> r = new ArrayList<Proxy>();
		final String host = uri.getHost();

		String type = IProxyData.SOCKS_PROXY_TYPE;
		if ("http".equals(uri.getScheme()))
			type = IProxyData.HTTP_PROXY_TYPE;
		else if ("ftp".equals(uri.getScheme()))
			type = IProxyData.HTTP_PROXY_TYPE;
		else if ("https".equals(uri.getScheme()))
			type = IProxyData.HTTPS_PROXY_TYPE;

		final IProxyData data = service.getProxyDataForHost(host, type);
		if (data != null) {
			if (IProxyData.HTTP_PROXY_TYPE.equals(data.getType()))
				addProxy(r, Proxy.Type.HTTP, data);
			else if (IProxyData.HTTPS_PROXY_TYPE.equals(data.getType()))
				addProxy(r, Proxy.Type.HTTP, data);
			else if (IProxyData.SOCKS_PROXY_TYPE.equals(data.getType()))
				addProxy(r, Proxy.Type.SOCKS, data);
		}
		if (r.isEmpty())
			r.add(Proxy.NO_PROXY);
		return r;
	}

	private void addProxy(final ArrayList<Proxy> r, final Proxy.Type type,
			final IProxyData d) {
		try {
			r.add(new Proxy(type, new InetSocketAddress(InetAddress.getByName(d
					.getHost()), d.getPort())));
		} catch (UnknownHostException uhe) {
			// Oh well.
		}
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		// Don't tell Eclipse.
	}
}
