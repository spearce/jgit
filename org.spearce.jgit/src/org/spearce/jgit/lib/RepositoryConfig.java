/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Thad Hughes <thadh@thad.corp.google.com>
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spearce.jgit.util.FS;
import org.spearce.jgit.util.SystemReader;

/**
 * An object representing the Git config file.
 *
 * This can be either the repository specific file or the user global
 * file depending on how it is instantiated.
 */
public class RepositoryConfig {
	/**
	 * Obtain a new configuration instance for ~/.gitconfig.
	 *
	 * @return a new configuration instance to read the user's global
	 *         configuration file from their home directory.
	 */
	public static RepositoryConfig openUserConfig() {
		return systemReader.openUserConfig();
	}

	private final RepositoryConfig baseConfig;

	/** Section name for a remote configuration */
	public static final String REMOTE_SECTION = "remote";

	/** Section name for a branch configuration. */
	public static final String BRANCH_SECTION = "branch";

	private final File configFile;

	private boolean readFile;

	private CoreConfig core;

	private TransferConfig transfer;

	private List<Entry> entries;

	private Map<String, Object> byName;

	private static String hostname;

	private static final String MAGIC_EMPTY_VALUE = "%%magic%%empty%%";

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
		baseConfig = base;
		configFile = cfgLocation;
		clear();
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
	 * Obtain an integer value from the configuration.
	 *
	 * @param section
	 *            section the key is grouped within.
	 * @param name
	 *            name of the key to get.
	 * @param defaultValue
	 *            default value to return if no value was present.
	 * @return an integer value from the configuration, or defaultValue.
	 */
	public int getInt(final String section, final String name,
			final int defaultValue) {
		return getInt(section, null, name, defaultValue);
	}

	/**
	 * Obtain an integer value from the configuration.
	 *
	 * @param section
	 *            section the key is grouped within.
	 * @param subsection
	 *            subsection name, such a remote or branch name.
	 * @param name
	 *            name of the key to get.
	 * @param defaultValue
	 *            default value to return if no value was present.
	 * @return an integer value from the configuration, or defaultValue.
	 */
	public int getInt(final String section, String subsection,
			final String name, final int defaultValue) {
		final String str = getString(section, subsection, name);
		if (str == null)
			return defaultValue;

		String n = str.trim();
		if (n.length() == 0)
			return defaultValue;

		int mul = 1;
		switch (Character.toLowerCase(n.charAt(n.length() - 1))) {
		case 'g':
			mul = 1024 * 1024 * 1024;
			break;
		case 'm':
			mul = 1024 * 1024;
			break;
		case 'k':
			mul = 1024;
			break;
		}
		if (mul > 1)
			n = n.substring(0, n.length() - 1).trim();
		if (n.length() == 0)
			return defaultValue;

		try {
			return mul * Integer.parseInt(n);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid integer value: "
					+ section + "." + name + "=" + str);
		}
	}

	/**
	 * Get a boolean value from the git config
	 *
	 * @param section
	 *            section the key is grouped within.
	 * @param name
	 *            name of the key to get.
	 * @param defaultValue
	 *            default value to return if no value was present.
	 * @return true if any value or defaultValue is true, false for missing or
	 *         explicit false
	 */
	public boolean getBoolean(final String section, final String name,
			final boolean defaultValue) {
		return getBoolean(section, null, name, defaultValue);
	}

	/**
	 * Get a boolean value from the git config
	 *
	 * @param section
	 *            section the key is grouped within.
	 * @param subsection
	 *            subsection name, such a remote or branch name.
	 * @param name
	 *            name of the key to get.
	 * @param defaultValue
	 *            default value to return if no value was present.
	 * @return true if any value or defaultValue is true, false for missing or
	 *         explicit false
	 */
	public boolean getBoolean(final String section, String subsection,
			final String name, final boolean defaultValue) {
		String n = getRawString(section, subsection, name);
		if (n == null)
			return defaultValue;

		if (MAGIC_EMPTY_VALUE.equals(n)
				|| "yes".equalsIgnoreCase(n)
				|| "true".equalsIgnoreCase(n)
				|| "1".equals(n)
				|| "on".equalsIgnoreCase(n)) {
			return true;

		} else if ("no".equalsIgnoreCase(n)
				|| "false".equalsIgnoreCase(n)
				|| "0".equals(n)
				|| "off".equalsIgnoreCase(n)) {
			return false;

		} else {
			throw new IllegalArgumentException("Invalid boolean value: "
					+ section + "." + name + "=" + n);
		}
	}

	/**
	 * @param section
	 * @param subsection
	 * @param name
	 * @return a String value from git config.
	 */
	public String getString(final String section, String subsection, final String name) {
		String val = getRawString(section, subsection, name);
		if (MAGIC_EMPTY_VALUE.equals(val)) {
			return "";
		}
		return val;
	}

	/**
	 * @param section
	 * @param subsection
	 * @param name
	 * @return array of zero or more values from the configuration.
	 */
	public String[] getStringList(final String section, String subsection,
			final String name) {
		final Object o = getRawEntry(section, subsection, name);
		if (o instanceof List) {
			final List lst = (List) o;
			final String[] r = new String[lst.size()];
			for (int i = 0; i < r.length; i++) {
				final String val = ((Entry) lst.get(i)).value;
				r[i] = MAGIC_EMPTY_VALUE.equals(val) ? "" : val;
			}
			return r;
		}

		if (o instanceof Entry) {
			final String val = ((Entry) o).value;
			return new String[] { MAGIC_EMPTY_VALUE.equals(val) ? "" : val };
		}

		if (baseConfig != null)
			return baseConfig.getStringList(section, subsection, name);
		return new String[0];
	}

	/**
	 * @param section
	 *            section to search for.
	 * @return set of all subsections of specified section within this
	 *         configuration and its base configuration; may be empty if no
	 *         subsection exists.
	 */
	public Set<String> getSubsections(final String section) {
		final Set<String> result = new HashSet<String>();

		for (final Entry e : entries) {
			if (section.equalsIgnoreCase(e.base) && e.extendedBase != null)
				result.add(e.extendedBase);
		}
		if (baseConfig != null)
			result.addAll(baseConfig.getSubsections(section));
		return result;
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

	private String getRawString(final String section, final String subsection,
			final String name) {
		final Object o = getRawEntry(section, subsection, name);
		if (o instanceof List) {
			return ((Entry) ((List) o).get(0)).value;
		} else if (o instanceof Entry) {
			return ((Entry) o).value;
		} else if (baseConfig != null)
			return baseConfig.getRawString(section, subsection, name);
		else
			return null;
	}

	private Object getRawEntry(final String section, final String subsection,
			final String name) {
		if (!readFile) {
			try {
				load();
			} catch (FileNotFoundException err) {
				// Oh well. No sense in complaining about it.
				//
			} catch (IOException err) {
				err.printStackTrace();
			}
		}

		String ss;
		if (subsection != null)
			ss = "."+subsection.toLowerCase();
		else
			ss = "";
		final Object o;
		o = byName.get(section.toLowerCase() + ss + "." + name.toLowerCase());
		return o;
	}

	/**
	 * Add or modify a configuration value. The parameters will result in a
	 * configuration entry like this.
	 *
	 * <pre>
	 * [section &quot;subsection&quot;]
	 *         name = value
	 * </pre>
	 *
	 * @param section
	 *            section name, e.g "branch"
	 * @param subsection
	 *            optional subsection value, e.g. a branch name
	 * @param name
	 *            parameter name, e.g. "filemode"
	 * @param value
	 *            parameter value
	 */
	public void setInt(final String section, final String subsection,
			final String name, final int value) {
		final String s;

		if ((value % (1024 * 1024 * 1024)) == 0)
			s = String.valueOf(value / (1024 * 1024 * 1024)) + " g";
		else if ((value % (1024 * 1024)) == 0)
			s = String.valueOf(value / (1024 * 1024)) + " m";
		else if ((value % 1024) == 0)
			s = String.valueOf(value / 1024) + " k";
		else
			s = String.valueOf(value);

		setString(section, subsection, name, s);
	}

	/**
	 * Add or modify a configuration value. The parameters will result in a
	 * configuration entry like this.
	 *
	 * <pre>
	 * [section &quot;subsection&quot;]
	 *         name = value
	 * </pre>
	 *
	 * @param section
	 *            section name, e.g "branch"
	 * @param subsection
	 *            optional subsection value, e.g. a branch name
	 * @param name
	 *            parameter name, e.g. "filemode"
	 * @param value
	 *            parameter value
	 */
	public void setBoolean(final String section, final String subsection,
			final String name, final boolean value) {
		setString(section, subsection, name, value ? "true" : "false");
	}

	/**
	 * Add or modify a configuration value. The parameters will result in a
	 * configuration entry like this.
	 *
	 * <pre>
	 * [section &quot;subsection&quot;]
	 *         name = value
	 * </pre>
	 *
	 * @param section
	 *            section name, e.g "branch"
	 * @param subsection
	 *            optional subsection value, e.g. a branch name
	 * @param name
	 *            parameter name, e.g. "filemode"
	 * @param value
	 *            parameter value, e.g. "true"
	 */
	public void setString(final String section, final String subsection,
			final String name, final String value) {
		setStringList(section, subsection, name, Collections
				.singletonList(value));
	}

	/**
	 * Remove a configuration value.
	 * 
	 * @param section
	 *            section name, e.g "branch"
	 * @param subsection
	 *            optional subsection value, e.g. a branch name
	 * @param name
	 *            parameter name, e.g. "filemode"
	 */
	public void unsetString(final String section, final String subsection,
			final String name) {
		setStringList(section, subsection, name, Collections
				.<String> emptyList());
	}

	/**
	 * Set a configuration value.
	 * 
	 * <pre>
	 * [section &quot;subsection&quot;]
	 *         name = value
	 * </pre>
	 * 
	 * @param section
	 *            section name, e.g "branch"
	 * @param subsection
	 *            optional subsection value, e.g. a branch name
	 * @param name
	 *            parameter name, e.g. "filemode"
	 * @param values
	 *            list of zero or more values for this key.
	 */
	public void setStringList(final String section, final String subsection,
			final String name, final List<String> values) {
		// Update our parsed cache of values for future reference.
		//
		String key = section.toLowerCase();
		if (subsection != null)
			key += "." + subsection.toLowerCase();
		key += "." + name.toLowerCase();
		if (values.size() == 0)
			byName.remove(key);
		else if (values.size() == 1) {
			final Entry e = new Entry();
			e.base = section;
			e.extendedBase = subsection;
			e.name = name;
			e.value = values.get(0);
			byName.put(key, e);
		} else {
			final ArrayList<Entry> eList = new ArrayList<Entry>(values.size());
			for (final String v : values) {
				final Entry e = new Entry();
				e.base = section;
				e.extendedBase = subsection;
				e.name = name;
				e.value = v;
				eList.add(e);
			}
			byName.put(key, eList);
		}

		int entryIndex = 0;
		int valueIndex = 0;
		int insertPosition = -1;

		// Reset the first n Entry objects that match this input name.
		//
		while (entryIndex < entries.size() && valueIndex < values.size()) {
			final Entry e = entries.get(entryIndex++);
			if (e.match(section, subsection, name)) {
				e.value = values.get(valueIndex++);
				insertPosition = entryIndex;
			}
		}

		// Remove any extra Entry objects that we no longer need.
		//
		if (valueIndex == values.size() && entryIndex < entries.size()) {
			while (entryIndex < entries.size()) {
				final Entry e = entries.get(entryIndex++);
				if (e.match(section, subsection, name))
					entries.remove(--entryIndex);
			}
		}

		// Insert new Entry objects for additional/new values.
		//
		if (valueIndex < values.size() && entryIndex == entries.size()){
			if (insertPosition < 0) {
				// We didn't find a matching key above, but maybe there
				// is already a section available that matches.  Insert
				// after the last key of that section.
				//
				insertPosition = findSectionEnd(section, subsection);
			}
			if (insertPosition < 0) {
				// We didn't find any matching section header for this key,
				// so we must create a new section header at the end.
				//
				final Entry e = new Entry();
				e.prefix = null;
				e.suffix = null;
				e.base = section;
				e.extendedBase = subsection;
				entries.add(e);
				insertPosition = entries.size();
			}
			while (valueIndex < values.size()) {
				final Entry e = new Entry();
				e.prefix = null;
				e.suffix = null;
				e.base = section;
				e.extendedBase = subsection;
				e.name = name;
				e.value = values.get(valueIndex++);
				entries.add(insertPosition++, e);
			}
		}
	}

	private int findSectionEnd(final String section, final String subsection) {
		for (int i = 0; i < entries.size(); i++) {
			Entry e = entries.get(i);
			if (e.match(section, subsection, null)) {
				i++;
				while (i < entries.size()) {
					e = entries.get(i);
					if (e.match(section, subsection, e.name))
						i++;
					else
						break;
				}
				return i;
			}
		}
		return -1;
	}

	/**
	 * Create a new default config
	 */
	public void create() {
		Entry e;

		clear();
		readFile = true;

		e = new Entry();
		e.base = "core";
		add(e);

		e = new Entry();
		e.base = "core";
		e.name = "repositoryformatversion";
		e.value = "0";
		add(e);

		e = new Entry();
		e.base = "core";
		e.name = "filemode";
		e.value = "true";
		add(e);

		core = new CoreConfig(this);
		transfer = new TransferConfig(this);
	}

	/**
	 * Save config data to the git config file
	 *
	 * @throws IOException
	 */
	public void save() throws IOException {
		final File tmp = new File(configFile.getParentFile(), configFile
				.getName()
				+ ".lock");
		final PrintWriter r = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(tmp),
						Constants.CHARSET))) {
			@Override
			public void println() {
				print('\n');
			}
		};
		boolean ok = false;
		try {
			final Iterator<Entry> i = entries.iterator();
			while (i.hasNext()) {
				final Entry e = i.next();
				if (e.prefix != null) {
					r.print(e.prefix);
				}
				if (e.base != null && e.name == null) {
					r.print('[');
					r.print(e.base);
					if (e.extendedBase != null) {
						r.print(' ');
						r.print('"');
						r.print(escapeValue(e.extendedBase));
						r.print('"');
					}
					r.print(']');
				} else if (e.base != null && e.name != null) {
					if (e.prefix == null || "".equals(e.prefix)) {
						r.print('\t');
					}
					r.print(e.name);
					if (e.value != null) {
						if (!MAGIC_EMPTY_VALUE.equals(e.value)) {
							r.print(" = ");
							r.print(escapeValue(e.value));
						}
					}
					if (e.suffix != null) {
						r.print(' ');
					}
				}
				if (e.suffix != null) {
					r.print(e.suffix);
				}
				r.println();
			}
			ok = true;
			r.close();
			if (!tmp.renameTo(configFile)) {
				configFile.delete();
				if (!tmp.renameTo(configFile))
					throw new IOException("Cannot save config file " + configFile + ", rename failed");
			}
		} finally {
			r.close();
			if (tmp.exists() && !tmp.delete()) {
				System.err.println("(warning) failed to delete tmp config file: " + tmp);
			}
		}
		readFile = ok;
	}

	/**
	 * Read the config file
	 * @throws IOException
	 */
	public void load() throws IOException {
		clear();
		readFile = true;
		final BufferedReader r = new BufferedReader(new InputStreamReader(
				new FileInputStream(configFile), Constants.CHARSET));
		try {
			Entry last = null;
			Entry e = new Entry();
			for (;;) {
				r.mark(1);
				int input = r.read();
				final char in = (char) input;
				if (-1 == input) {
					break;
				} else if ('\n' == in) {
					// End of this entry.
					add(e);
					if (e.base != null) {
						last = e;
					}
					e = new Entry();
				} else if (e.suffix != null) {
					// Everything up until the end-of-line is in the suffix.
					e.suffix += in;
				} else if (';' == in || '#' == in) {
					// The rest of this line is a comment; put into suffix.
					e.suffix = String.valueOf(in);
				} else if (e.base == null && Character.isWhitespace(in)) {
					// Save the leading whitespace (if any).
					if (e.prefix == null) {
						e.prefix = "";
					}
					e.prefix += in;
				} else if ('[' == in) {
					// This is a group header line.
					e.base = readBase(r);
					input = r.read();
					if ('"' == input) {
						e.extendedBase = readValue(r, true, '"');
						input = r.read();
					}
					if (']' != input) {
						throw new IOException("Bad group header.");
					}
					e.suffix = "";
				} else if (last != null) {
					// Read a value.
					e.base = last.base;
					e.extendedBase = last.extendedBase;
					r.reset();
					e.name = readName(r);
					if (e.name.endsWith("\n")) {
						e.name = e.name.substring(0, e.name.length()-1);
						e.value = MAGIC_EMPTY_VALUE;
					} else 
						e.value = readValue(r, false, -1);
				} else {
					throw new IOException("Invalid line in config file.");
				}
			}
		} finally {
			r.close();
		}

		core = new CoreConfig(this);
		transfer = new TransferConfig(this);
	}

	private void clear() {
		entries = new ArrayList<Entry>();
		byName = new HashMap<String, Object>();
	}

	@SuppressWarnings("unchecked")
	private void add(final Entry e) {
		entries.add(e);
		if (e.base != null) {
			final String b = e.base.toLowerCase();
			final String group;
			if (e.extendedBase != null) {
				group = b + "." + e.extendedBase;
			} else {
				group = b;
			}
			if (e.name != null) {
				final String n = e.name.toLowerCase();
				final String key = group + "." + n;
				final Object o = byName.get(key);
				if (o == null) {
					byName.put(key, e);
				} else if (o instanceof Entry) {
					final ArrayList<Object> l = new ArrayList<Object>();
					l.add(o);
					l.add(e);
					byName.put(key, l);
				} else if (o instanceof List) {
					((List<Entry>) o).add(e);
				}
			}
		}
	}

	private static String escapeValue(final String x) {
		boolean inquote = false;
		int lineStart = 0;
		final StringBuffer r = new StringBuffer(x.length());
		for (int k = 0; k < x.length(); k++) {
			final char c = x.charAt(k);
			switch (c) {
			case '\n':
				if (inquote) {
					r.append('"');
					inquote = false;
				}
				r.append("\\n\\\n");
				lineStart = r.length();
				break;

			case '\t':
				r.append("\\t");
				break;

			case '\b':
				r.append("\\b");
				break;

			case '\\':
				r.append("\\\\");
				break;

			case '"':
				r.append("\\\"");
				break;

			case ';':
			case '#':
				if (!inquote) {
					r.insert(lineStart, '"');
					inquote = true;
				}
				r.append(c);
				break;

			case ' ':
				if (!inquote && r.length() > 0
						&& r.charAt(r.length() - 1) == ' ') {
					r.insert(lineStart, '"');
					inquote = true;
				}
				r.append(' ');
				break;

			default:
				r.append(c);
				break;
			}
		}
		if (inquote) {
			r.append('"');
		}
		return r.toString();
	}

	private static String readBase(final BufferedReader r) throws IOException {
		final StringBuffer base = new StringBuffer();
		for (;;) {
			r.mark(1);
			int c = r.read();
			if (c < 0) {
				throw new IOException("Unexpected end of config file.");
			} else if (']' == c) {
				r.reset();
				break;
			} else if (' ' == c || '\t' == c) {
				for (;;) {
					r.mark(1);
					c = r.read();
					if (c < 0) {
						throw new IOException("Unexpected end of config file.");
					} else if ('"' == c) {
						r.reset();
						break;
					} else if (' ' == c || '\t' == c) {
						// Skipped...
					} else {
						throw new IOException("Bad base entry. : " + base + "," + c);
					}
				}
				break;
			} else if (Character.isLetterOrDigit((char) c) || '.' == c || '-' == c) {
				base.append((char) c);
			} else {
				throw new IOException("Bad base entry. : " + base + ", " + c);
			}
		}
		return base.toString();
	}

	private static String readName(final BufferedReader r) throws IOException {
		final StringBuffer name = new StringBuffer();
		for (;;) {
			r.mark(1);
			int c = r.read();
			if (c < 0) {
				throw new IOException("Unexpected end of config file.");
			} else if ('=' == c) {
				break;
			} else if (' ' == c || '\t' == c) {
				for (;;) {
					r.mark(1);
					c = r.read();
					if (c < 0) {
						throw new IOException("Unexpected end of config file.");
					} else if ('=' == c) {
						break;
					} else if (';' == c || '#' == c || '\n' == c) {
						r.reset();
						break;
					} else if (' ' == c || '\t' == c) {
						// Skipped...
					} else {
						throw new IOException("Bad entry delimiter.");
					}
				}
				break;
			} else if (Character.isLetterOrDigit((char) c) || c == '-') {
				// From the git-config man page:
				//     The variable names are case-insensitive and only
				//     alphanumeric characters and - are allowed.
				name.append((char) c);
			} else if ('\n' == c) {
				r.reset();
				name.append((char) c);
				break;
			} else {
				throw new IOException("Bad config entry name: " + name + (char) c);
			}
		}
		return name.toString();
	}

	private static String readValue(final BufferedReader r, boolean quote,
			final int eol) throws IOException {
		final StringBuffer value = new StringBuffer();
		boolean space = false;
		for (;;) {
			r.mark(1);
			int c = r.read();
			if (c < 0) {
				if (value.length() == 0)
					throw new IOException("Unexpected end of config file.");
				break;
			}
			if ('\n' == c) {
				if (quote) {
					throw new IOException("Newline in quotes not allowed.");
				}
				r.reset();
				break;
			}
			if (eol == c) {
				break;
			}
			if (!quote) {
				if (Character.isWhitespace((char) c)) {
					space = true;
					continue;
				}
				if (';' == c || '#' == c) {
					r.reset();
					break;
				}
			}
			if (space) {
				if (value.length() > 0) {
					value.append(' ');
				}
				space = false;
			}
			if ('\\' == c) {
				c = r.read();
				switch (c) {
				case -1:
					throw new IOException("End of file in escape.");
				case '\n':
					continue;
				case 't':
					value.append('\t');
					continue;
				case 'b':
					value.append('\b');
					continue;
				case 'n':
					value.append('\n');
					continue;
				case '\\':
					value.append('\\');
					continue;
				case '"':
					value.append('"');
					continue;
				default:
					throw new IOException("Bad escape: " + ((char) c));
				}
			}
			if ('"' == c) {
				quote = !quote;
				continue;
			}
			value.append((char) c);
		}
		return value.length() > 0 ? value.toString() : null;
	}

	public String toString() {
		return "RepositoryConfig[" + configFile.getPath() + "]";
	}

	static class Entry {
		String prefix;

		String base;

		String extendedBase;

		String name;

		String value;

		String suffix;

		boolean match(final String aBase, final String aExtendedBase,
				final String aName) {
			return eq(base, aBase) && eq(extendedBase, aExtendedBase)
					&& eq(name, aName);
		}

		private static boolean eq(final String a, final String b) {
			if (a == null && b == null)
				return true;
			if (a == null || b == null)
				return false;
			return a.equalsIgnoreCase(b);
		}
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
