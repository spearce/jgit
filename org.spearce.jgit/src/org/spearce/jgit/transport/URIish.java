/*
 *  Copyright (C) 2008  Robin Rosenberg
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

import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This URI like construct used for referencing Git archives over the net, as
 * well as locally stored archives. The most important difference compared to
 * RFC 2396 URI's is that no URI encoding/decoding ever takes place. A space or
 * any special character is written as-is.
 */
public class URIish {
	private static final Pattern FULL_URI = Pattern
			.compile("^(?:([a-z+]+)://(?:([^/]+?)(?::([^/]+?))?@)?(?:([^/]+?))?(?::(\\d+))?)?((?:[A-Za-z]:)?/.+)$");

	private static final Pattern SCP_URI = Pattern
			.compile("^(?:([^@]+?)@)?([^:]+?):(.+)$");

	private String scheme;

	private String path;

	private String user;

	private String pass;

	private int port = -1;

	private String host;

	/**
	 * Parse and construct an {@link URIish} from a string
	 * 
	 * @param s
	 * @throws URISyntaxException
	 */
	public URIish(String s) throws URISyntaxException {
		Matcher matcher = FULL_URI.matcher(s);
		if (matcher.matches()) {
			scheme = matcher.group(1);
			user = matcher.group(2);
			pass = matcher.group(3);
			host = matcher.group(4);
			if (matcher.group(5) != null)
				port = Integer.parseInt(matcher.group(5));
			path = matcher.group(6);
			if (path.length() >= 3
			&& path.charAt(0) == '/'
			&& path.charAt(2) == ':'
			&& (path.charAt(1) >= 'A' && path.charAt(1) <= 'Z'
			 || path.charAt(1) >= 'a' && path.charAt(1) <= 'z'))
				path = path.substring(1);
		} else {
			matcher = SCP_URI.matcher(s);
			if (matcher.matches()) {
				user = matcher.group(1);
				host = matcher.group(2);
				path = matcher.group(3);
			} else
				throw new URISyntaxException(s, "Cannot parse Git URI-ish");
		}
	}

	/** Create an empty, non-configured URI. */
	public URIish() {
		// Configure nothing.
	}

	private URIish(final URIish u) {
		this.scheme = u.scheme;
		this.path = u.path;
		this.user = u.user;
		this.pass = u.pass;
		this.port = u.port;
		this.host = u.host;
	}

	/**
	 * @return true if this URI references a repository on another system.
	 */
	public boolean isRemote() {
		return getHost() != null;
	}

	/**
	 * @return host name part or null
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Return a new URI matching this one, but with a different host.
	 * 
	 * @param n
	 *            the new value for host.
	 * @return a new URI with the updated value.
	 */
	public URIish setHost(final String n) {
		final URIish r = new URIish(this);
		r.host = n;
		return r;
	}

	/**
	 * @return protocol name or null for local references
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Return a new URI matching this one, but with a different scheme.
	 * 
	 * @param n
	 *            the new value for scheme.
	 * @return a new URI with the updated value.
	 */
	public URIish setScheme(final String n) {
		final URIish r = new URIish(this);
		r.scheme = n;
		return r;
	}

	/**
	 * @return path name component
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Return a new URI matching this one, but with a different path.
	 * 
	 * @param n
	 *            the new value for path.
	 * @return a new URI with the updated value.
	 */
	public URIish setPath(final String n) {
		final URIish r = new URIish(this);
		r.path = n;
		return r;
	}

	/**
	 * @return user name requested for transfer or null
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Return a new URI matching this one, but with a different user.
	 * 
	 * @param n
	 *            the new value for user.
	 * @return a new URI with the updated value.
	 */
	public URIish setUser(final String n) {
		final URIish r = new URIish(this);
		r.user = n;
		return r;
	}

	/**
	 * @return password requested for transfer or null
	 */
	public String getPass() {
		return pass;
	}

	/**
	 * Return a new URI matching this one, but with a different password.
	 * 
	 * @param n
	 *            the new value for password.
	 * @return a new URI with the updated value.
	 */
	public URIish setPass(final String n) {
		final URIish r = new URIish(this);
		r.pass = n;
		return r;
	}

	/**
	 * @return port number requested for transfer or -1 if not explicit
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Return a new URI matching this one, but with a different port.
	 * 
	 * @param n
	 *            the new value for port.
	 * @return a new URI with the updated value.
	 */
	public URIish setPort(final int n) {
		final URIish r = new URIish(this);
		r.port = n > 0 ? n : -1;
		return r;
	}

	public int hashCode() {
		int hc = 0;
		if (getScheme() != null)
			hc = hc * 31 + getScheme().hashCode();
		if (getUser() != null)
			hc = hc * 31 + getUser().hashCode();
		if (getPass() != null)
			hc = hc * 31 + getPass().hashCode();
		if (getHost() != null)
			hc = hc * 31 + getHost().hashCode();
		if (getPort() > 0)
			hc = hc * 31 + getPort();
		if (getPath() != null)
			hc = hc * 31 + getPath().hashCode();
		return hc;
	}

	public boolean equals(final Object obj) {
		if (!(obj instanceof URIish))
			return false;
		final URIish b = (URIish) obj;
		if (!eq(getScheme(), b.getScheme()))
			return false;
		if (!eq(getUser(), b.getUser()))
			return false;
		if (!eq(getPass(), b.getPass()))
			return false;
		if (!eq(getHost(), b.getHost()))
			return false;
		if (getPort() != b.getPort())
			return false;
		if (!eq(getPath(), b.getPath()))
			return false;
		return true;
	}

	private static boolean eq(final String a, final String b) {
		if (a == b)
			return true;
		if (a == null || b == null)
			return false;
		return a.equals(b);
	}

	public String toString() {
		final StringBuilder r = new StringBuilder();
		if (getScheme() != null) {
			r.append(getScheme());
			r.append("://");
		}

		if (getUser() != null) {
			r.append(getUser());
			if (getPass() != null) {
				r.append(':');
				r.append(getPass());
			}
		}

		if (getHost() != null) {
			if (getUser() != null)
				r.append('@');
			r.append(getHost());
			if (getScheme() != null && getPort() > 0) {
				r.append(':');
				r.append(getPort());
			}
		}

		if (getPath() != null) {
			if (getScheme() != null) {
				if (!getPath().startsWith("/"))
					r.append('/');
			} else if (getHost() != null)
				r.append(':');
			r.append(getPath());
		}

		return r.toString();
	}
}
