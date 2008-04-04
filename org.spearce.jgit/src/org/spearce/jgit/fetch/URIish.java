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
package org.spearce.jgit.fetch;

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
	private String protocol;

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
		Pattern p = Pattern
				.compile("^(?:([a-z]+)://(?:([^/]+?)(?::([^/]+?))?@)?(?:([^/]+?))?(?::(\\d+))?)?((?:[A-Za-z]:)?/.+)$");
		Matcher matcher = p.matcher(s);
		if (matcher.matches()) {
			protocol = matcher.group(1);
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
		} else
			throw new URISyntaxException(s, "Cannot parse Git URI-ish");
	}

	/**
	 * @return host name part or null
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return protocol name or null for local references
	 */
	public String getScheme() {
		return protocol;
	}

	/**
	 * @return path name component
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return user name requested for transfer or null
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @return password requested for transfer or null
	 */
	public String getPass() {
		return pass;
	}

	/**
	 * @return port number requested for transfer or -1 if not explicit
	 */
	public int getPort() {
		return port;
	}
}
