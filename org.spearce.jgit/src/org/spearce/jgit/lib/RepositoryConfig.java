/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Thad Hughes <thadh@thad.corp.google.com>
 * Copyright (C) 2009, JetBrains s.r.o.
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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.spearce.jgit.util.FS;
import org.spearce.jgit.util.SystemReader;

/**
 * An object representing the Git config file.
 *
 * This can be either the repository specific file or the user global
 * file depending on how it is instantiated.
 */
public class RepositoryConfig extends FileBasedConfig {
	/**
	 * Obtain a new configuration instance for ~/.gitconfig.
	 *
	 * @return a new configuration instance to read the user's global
	 *         configuration file from their home directory.
	 */
	public static RepositoryConfig openUserConfig() {
		return systemReader.openUserConfig();
	}


	/** Section name for a remote configuration */
	public static final String REMOTE_SECTION = "remote";

	/** Section name for a branch configuration. */
	public static final String BRANCH_SECTION = "branch";

	CoreConfig core;

	TransferConfig transfer;

	private static String hostname;

	// default system reader gets the value from the system
	private static SystemReader systemReader = new SystemReader() {
		public String getenv(String variable) {
			return System.getenv(variable);
		}
		public String getProperty(String key) {
			return System.getProperty(key);
		}
		public RepositoryConfig openUserConfig() {
			return new RepositoryConfig(null, new File(FS.userHome(), ".gitconfig"));
		}
	};

	RepositoryConfig(final Repository repo) {
		this(openUserConfig(), FS.resolve(repo.getDirectory(), "config"));
	}

	/**
	 * Create a Git configuration file reader/writer/cache for a specific file.
	 *
	 * @param base
	 *            configuration that provides default values if this file does
	 *            not set/override a particular key. Often this is the user's
	 *            global configuration file, or the system level configuration.
	 * @param cfgLocation
	 *            path of the file to load (or save).
	 */
	public RepositoryConfig(final RepositoryConfig base, final File cfgLocation) {
		super(base, cfgLocation);
	}

	/**
	 * @return Core configuration values
	 */
	public CoreConfig getCore() {
		return core;
	}

	/**
	 * @return transfer, fetch and receive configuration values
	 */
	public TransferConfig getTransfer() {
		return transfer;
	}

	/**
	 * @return the author name as defined in the git variables
	 *         and configurations. If no name could be found, try
	 *         to use the system user name instead.
	 */
	public String getAuthorName() {
		return getUsernameInternal(Constants.GIT_AUTHOR_NAME_KEY);
	}

	/**
	 * @return the committer name as defined in the git variables
	 *         and configurations. If no name could be found, try
	 *         to use the system user name instead.
	 */
	public String getCommitterName() {
		return getUsernameInternal(Constants.GIT_COMMITTER_NAME_KEY);
	}

	private String getUsernameInternal(String gitVariableKey) {
		// try to get the user name from the local and global configurations.
		String username = getString("user", null, "name");

		if (username == null) {
			// try to get the user name for the system property GIT_XXX_NAME
			username = systemReader.getenv(gitVariableKey);
		}
		if (username == null) {
			// get the system user name
			username = systemReader.getProperty(Constants.OS_USER_NAME_KEY);
		}
		if (username == null) {
			username = Constants.UNKNOWN_USER_DEFAULT;
		}
		return username;
	}

	/**
	 * @return the author email as defined in git variables and
	 *         configurations. If no email could be found, try to
	 *         propose one default with the user name and the
	 *         host name.
	 */
	public String getAuthorEmail() {
		return getUserEmailInternal(Constants.GIT_AUTHOR_EMAIL_KEY);
	}

	/**
	 * @return the committer email as defined in git variables and
	 *         configurations. If no email could be found, try to
	 *         propose one default with the user name and the
	 *         host name.
	 */
	public String getCommitterEmail() {
		return getUserEmailInternal(Constants.GIT_COMMITTER_EMAIL_KEY);
	}

	private String getUserEmailInternal(String gitVariableKey) {
		// try to get the email from the local and global configurations.
		String email = getString("user", null, "email");

		if (email == null) {
			// try to get the email for the system property GIT_XXX_EMAIL
			email = systemReader.getenv(gitVariableKey);
		}

		if (email == null) {
			// try to construct an email
			String username = systemReader.getProperty(Constants.OS_USER_NAME_KEY);
			if (username == null){
				username = Constants.UNKNOWN_USER_DEFAULT;
			}
			email = username + "@" + getHostname();
		}

		return email;
	}

	/**
	 * Create a new default config
	 */
	public void create() {
		clear();
		setFileRead(true);
		setString("core", null, "repositoryformatversion", "0");
		setString("core", null, "filemode", "true");

		core = new CoreConfig(this);
		transfer = new TransferConfig(this);
	}

	@Override
	public void load() throws IOException {
		super.load();
		core = new CoreConfig(this);
		transfer = new TransferConfig(this);
	}

	/**
	 * Gets the hostname of the local host.
	 * If no hostname can be found, the hostname is set to the default value "localhost".
	 * @return the canonical hostname
	 */
	private static String getHostname() {
		if (hostname == null) {
			try {
				InetAddress localMachine = InetAddress.getLocalHost();
				hostname = localMachine.getCanonicalHostName();
			} catch (UnknownHostException e) {
				// we do nothing
				hostname = "localhost";
			}
			assert hostname != null;
		}
		return hostname;
	}

	/**
	 * Overrides the default system reader by a custom one.
	 * @param newSystemReader new system reader
	 */
	public static void setSystemReader(SystemReader newSystemReader) {
		systemReader = newSystemReader;
	}
}
