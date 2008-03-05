/*
 *  Copyright (C) 2007  Robin Rosenberg
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
package org.spearce.jgit.lib;

/**
 * Information about how to synchronize with a remote Git repository.
 *
 * A remote is stored in the <GIT_DIR>/config as
 *
 * <pre>
 *  [remote &quot;name&quot;]
 *     url = URL:ish
 *     fetch = [+]remoteref:localref
 * </pre>
 *
 * There are more variants but we do not support them here yet.
 */
public class RemoteSpec {

	static class Info {
		boolean overwriteAlways;

		boolean matchAny;

		String remoteRef;

		String localRef;
	}

	final Info fetch = new Info();

	Info push = null;

	private final String name;

	private final String url;

	/**
	 * @return name of remote. This is a local short identifier
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the URL:ish location of the remote Git repository
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the local ref part used for fetch heads info
	 */
	public String getFetchLocalRef() {
		return fetch.localRef;
	}

	/**
	 * @return the remote ref part used for fetching refs from the remote repo
	 */
	public String getFetchRemoteRef() {
		return fetch.remoteRef;
	}

	/**
	 * @return whether the fetch matches all branches under the ref or just the
	 *         named ref
	 */
	public boolean isFetchMatchAny() {
		return fetch.matchAny;
	}

	/**
	 * @return whether the tracking branch is always updated, or only when the
	 *         update is a fast forward
	 */
	public boolean isFetchOverwriteAlways() {
		return fetch.overwriteAlways;
	}

	/**
	 * Create a representation of a git remote specification.
	 *
	 * @param name A local short identifier
	 * @param url The URL:ish used for fetching / pushing
	 * @param fetchPattern refspec for fetching
	 * @param pushPattern refspec for pushing or null
	 */
	public RemoteSpec(final String name, final String url, final String fetchPattern,
			final String pushPattern) {
		this.name = name;
		this.url = url;
		parse(fetchPattern, fetch);
		if (pushPattern != null) {
			push = new Info();
			parse(pushPattern, push);
		}
	}

	private void parse(final String fetchSpec, final Info info) {
		int p = 0;
		if (fetchSpec.charAt(p) == '+') {
			info.overwriteAlways = true;
			++p;
		}
		int cp = fetchSpec.indexOf(':');
		if (cp < 0)
			throw new IllegalArgumentException("Bad remote format " + fetchSpec);
		info.remoteRef = fetchSpec.substring(p, cp);
		info.localRef = fetchSpec.substring(cp + 1);
		if (info.remoteRef.endsWith("/*")) {
			info.matchAny = true;
			info.remoteRef = info.remoteRef.substring(0, info.remoteRef
					.length() - 2);
		}
		if (info.localRef.endsWith("/*")) {
			if (!info.matchAny)
				throw new IllegalArgumentException("Bad remote format "
						+ fetchSpec);
			info.localRef = info.localRef.substring(0,
					info.localRef.length() - 2);
		} else
			if (info.matchAny)
				throw new IllegalArgumentException("Bad remote format " + fetchSpec);

	}
}
