/*
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com>
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

package org.spearce.jgit.lib;

import java.util.HashMap;
import java.util.Map;

import org.spearce.jgit.util.SystemReader;

class MockSystemReader extends SystemReader {
	final Map<String, String> values = new HashMap<String, String>();

	FileBasedConfig userGitConfig;

	MockSystemReader() {
		init(Constants.OS_USER_NAME_KEY);
		init(Constants.GIT_AUTHOR_NAME_KEY);
		init(Constants.GIT_AUTHOR_EMAIL_KEY);
		init(Constants.GIT_COMMITTER_NAME_KEY);
		init(Constants.GIT_COMMITTER_EMAIL_KEY);
		userGitConfig = new FileBasedConfig(null);
	}

	private void init(final String n) {
		values.put(n, n);
	}

	public String getenv(String variable) {
		return values.get(variable);
	}

	public String getProperty(String key) {
		return values.get(key);
	}

	public FileBasedConfig openUserConfig() {
		return userGitConfig;
	}

	public String getHostname() {
		return "fake.host.example.com";
	}
}
