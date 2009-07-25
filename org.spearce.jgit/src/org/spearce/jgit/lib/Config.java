/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Thad Hughes <thadh@thad.corp.google.com>
 * Copyright (C) 2009, JetBrains s.r.o.
 * Copyright (C) 2009, Google, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spearce.jgit.errors.ConfigInvalidException;
import org.spearce.jgit.util.StringUtils;


/**
 * Git style {@code .config}, {@code .gitconfig}, {@code .gitmodules} file.
 */
public class Config {
	private static final long KiB = 1024;
	private static final long MiB = 1024 * KiB;
	private static final long GiB = 1024 * MiB;

	private List<Entry> entries;

	final Config baseConfig;

	/**
	 * Map from name to values
	 */
	private Map<String, Object> byName;

	/**
	 * Magic value indicating a missing entry.
	 * <p>
	 * This value is tested for reference equality in some contexts, so we
	 * must ensure it is a special copy of the empty string.  It also must
	 * be treated like the empty string.
	 */
	private static final String MAGIC_EMPTY_VALUE = new String();

	/** Create a configuration with no default fallback. */
	public Config() {
		this(null);
	}

	/**
	 * Create an empty configuration with a fallback for missing keys.
	 *
	 * @param defaultConfig
	 *            the base configuration to be consulted when a key is missing
	 *            from this configuration instance.
	 */
	public Config(Config defaultConfig) {
		baseConfig = defaultConfig;
		clear();
	}

	/**
	 * Escape the value before saving
	 *
	 * @param x
	 *            the value to escape
	 * @return the escaped value
	 */
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
		final long val = getLong(section, subsection, name, defaultValue);
		if (Integer.MIN_VALUE <= val && val <= Integer.MAX_VALUE)
			return (int) val;
		throw new IllegalArgumentException("Integer value " + section + "."
				+ name + " out of range");
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
	public long getLong(final String section, String subsection,
			final String name, final long defaultValue) {
		final String str = getString(section, subsection, name);
		if (str == null)
			return defaultValue;

		String n = str.trim();
		if (n.length() == 0)
			return defaultValue;

		long mul = 1;
		switch (StringUtils.toLowerCase(n.charAt(n.length() - 1))) {
		case 'g':
			mul = GiB;
			break;
		case 'm':
			mul = MiB;
			break;
		case 'k':
			mul = KiB;
			break;
		}
		if (mul > 1)
			n = n.substring(0, n.length() - 1).trim();
		if (n.length() == 0)
			return defaultValue;

		try {
			return mul * Long.parseLong(n);
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

		if (MAGIC_EMPTY_VALUE == n || StringUtils.equalsIgnoreCase("yes", n)
				|| StringUtils.equalsIgnoreCase("true", n)
				|| StringUtils.equalsIgnoreCase("1", n)
				|| StringUtils.equalsIgnoreCase("on", n)) {
			return true;

		} else if (StringUtils.equalsIgnoreCase("no", n)
				|| StringUtils.equalsIgnoreCase("false", n)
				|| StringUtils.equalsIgnoreCase("0", n)
				|| StringUtils.equalsIgnoreCase("off", n)) {
			return false;

		} else {
			throw new IllegalArgumentException("Invalid boolean value: "
					+ section + "." + name + "=" + n);
		}
	}

	/**
	 * Get string value
	 *
	 * @param section
	 *            the section
	 * @param subsection
	 *            the subsection for the value
	 * @param name
	 *            the key name
	 * @return a String value from git config.
	 */
	public String getString(final String section, String subsection,
			final String name) {
		return getRawString(section, subsection, name);
	}

	/**
	 * Get a list of string values
	 *
	 * @param section
	 *            the section
	 * @param subsection
	 *            the subsection for the value
	 * @param name
	 *            the key name
	 * @return array of zero or more values from the configuration.
	 */
	public String[] getStringList(final String section, String subsection,
			final String name) {
		final Object o = getRawEntry(section, subsection, name);
		if (o instanceof List) {
			final List lst = (List) o;
			final String[] r = new String[lst.size()];
			for (int i = 0; i < r.length; i++)
				r[i] = ((Entry) lst.get(i)).value;
			return r;
		}

		if (o instanceof Entry) {
			return new String[] { ((Entry) o).value };
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
			if (StringUtils.equalsIgnoreCase(section, e.section)
					&& e.subsection != null)
				result.add(e.subsection);
		}
		if (baseConfig != null)
			result.addAll(baseConfig.getSubsections(section));
		return result;
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
		return byName.get(concatenateKey(section, subsection, name));
	}

	/**
	 * Create simple a key name from the key components
	 *
	 * @param section
	 *            the section name
	 * @param subsection
	 *            the subsection name
	 * @param name
	 *            the key name
	 * @return a simple key name that have all components concatenated and the
	 *         case converted
	 */
	private static String concatenateKey(final String section,
			final String subsection, final String name) {
		String ss;
		if (subsection != null)
			ss = "." + subsection;
		else
			ss = "";
		return StringUtils.toLowerCase(section) + ss + "."
				+ StringUtils.toLowerCase(name);
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
		setLong(section, subsection, name, value);
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
	public void setLong(final String section, final String subsection,
			final String name, final long value) {
		final String s;

		if (value >= GiB && (value % GiB) == 0)
			s = String.valueOf(value / GiB) + " g";
		else if (value >= MiB && (value % MiB) == 0)
			s = String.valueOf(value / MiB) + " m";
		else if (value >= KiB && (value % KiB) == 0)
			s = String.valueOf(value / KiB) + " k";
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
	public void unset(final String section, final String subsection,
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
		String key = concatenateKey(section, subsection, name);
		if (values.size() == 0)
			byName.remove(key);
		else if (values.size() == 1) {
			final Entry e = new Entry();
			e.section = section;
			e.subsection = subsection;
			e.name = name;
			e.value = values.get(0);
			byName.put(key, e);
		} else {
			final ArrayList<Entry> eList = new ArrayList<Entry>(values.size());
			for (final String v : values) {
				final Entry e = new Entry();
				e.section = section;
				e.subsection = subsection;
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
		if (valueIndex < values.size() && entryIndex == entries.size()) {
			if (insertPosition < 0) {
				// We didn't find a matching key above, but maybe there
				// is already a section available that matches. Insert
				// after the last key of that section.
				//
				insertPosition = findSectionEnd(section, subsection);
			}
			if (insertPosition < 0) {
				// We didn't find any matching section header for this key,
				// so we must create a new section header at the end.
				//
				final Entry e = new Entry();
				e.section = section;
				e.subsection = subsection;
				entries.add(e);
				insertPosition = entries.size();
			}
			while (valueIndex < values.size()) {
				final Entry e = new Entry();
				e.section = section;
				e.subsection = subsection;
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
	 * @return this configuration, formatted as a Git style text file.
	 */
	public String toText() {
		final StringBuilder out = new StringBuilder();
		for (final Entry e : entries) {
			if (e.prefix != null)
				out.append(e.prefix);
			if (e.section != null && e.name == null) {
				out.append('[');
				out.append(e.section);
				if (e.subsection != null) {
					out.append(' ');
					out.append('"');
					out.append(escapeValue(e.subsection));
					out.append('"');
				}
				out.append(']');
			} else if (e.section != null && e.name != null) {
				if (e.prefix == null || "".equals(e.prefix))
					out.append('\t');
				out.append(e.name);
				if (e.value != null) {
					if (MAGIC_EMPTY_VALUE != e.value) {
						out.append(" = ");
						out.append(escapeValue(e.value));
					}
				}
				if (e.suffix != null)
					out.append(' ');
			}
			if (e.suffix != null)
				out.append(e.suffix);
			out.append('\n');
		}
		return out.toString();
	}

	/**
	 * Clear this configuration and reset to the contents of the parsed string.
	 *
	 * @param text
	 *            Git style text file listing configuration properties.
	 * @throws ConfigInvalidException
	 *             the text supplied is not formatted correctly. No changes were
	 *             made to {@code this}.
	 */
	public void fromText(final String text) throws ConfigInvalidException {
		entries = new ArrayList<Entry>();
		byName = new HashMap<String, Object>();

		final StringReader in = new StringReader(text);
		Entry last = null;
		Entry e = new Entry();
		for (;;) {
			int input = in.read();
			if (-1 == input)
				break;

			final char c = (char) input;
			if ('\n' == c) {
				// End of this entry.
				add(e);
				if (e.section != null)
					last = e;
				e = new Entry();

			} else if (e.suffix != null) {
				// Everything up until the end-of-line is in the suffix.
				e.suffix += c;

			} else if (';' == c || '#' == c) {
				// The rest of this line is a comment; put into suffix.
				e.suffix = String.valueOf(c);

			} else if (e.section == null && Character.isWhitespace(c)) {
				// Save the leading whitespace (if any).
				if (e.prefix == null)
					e.prefix = "";
				e.prefix += c;

			} else if ('[' == c) {
				// This is a section header.
				e.section = readSectionName(in);
				input = in.read();
				if ('"' == input) {
					e.subsection = readValue(in, true, '"');
					input = in.read();
				}
				if (']' != input)
					throw new ConfigInvalidException("Bad group header");
				e.suffix = "";

			} else if (last != null) {
				// Read a value.
				e.section = last.section;
				e.subsection = last.subsection;
				in.reset();
				e.name = readKeyName(in);
				if (e.name.endsWith("\n")) {
					e.name = e.name.substring(0, e.name.length() - 1);
					e.value = MAGIC_EMPTY_VALUE;
				} else
					e.value = readValue(in, false, -1);

			} else
				throw new ConfigInvalidException("Invalid line in config file");
		}
	}

	/**
	 * Clear the configuration file
	 */
	protected void clear() {
		entries = new ArrayList<Entry>();
		byName = new HashMap<String, Object>();
	}

	@SuppressWarnings("unchecked")
	private void add(final Entry e) {
		entries.add(e);
		if (e.section != null && e.name != null) {
			final String key = concatenateKey(e.section, e.subsection, e.name);
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

	private static String readSectionName(final StringReader in)
			throws ConfigInvalidException {
		final StringBuilder name = new StringBuilder();
		for (;;) {
			int c = in.read();
			if (c < 0)
				throw new ConfigInvalidException("Unexpected end of config file");

			if (']' == c) {
				in.reset();
				break;
			}

			if (' ' == c || '\t' == c) {
				for (;;) {
					c = in.read();
					if (c < 0)
						throw new ConfigInvalidException("Unexpected end of config file");

					if ('"' == c) {
						in.reset();
						break;
					}

					if (' ' == c || '\t' == c)
						continue; // Skipped...
					throw new ConfigInvalidException("Bad section entry: " + name);
				}
				break;
			}

			if (Character.isLetterOrDigit((char) c) || '.' == c || '-' == c)
				name.append((char) c);
			else
				throw new ConfigInvalidException("Bad section entry: " + name);
		}
		return name.toString();
	}

	private static String readKeyName(final StringReader in)
			throws ConfigInvalidException {
		final StringBuffer name = new StringBuffer();
		for (;;) {
			int c = in.read();
			if (c < 0)
				throw new ConfigInvalidException("Unexpected end of config file");

			if ('=' == c)
				break;

			if (' ' == c || '\t' == c) {
				for (;;) {
					c = in.read();
					if (c < 0)
						throw new ConfigInvalidException("Unexpected end of config file");

					if ('=' == c)
						break;

					if (';' == c || '#' == c || '\n' == c) {
						in.reset();
						break;
					}

					if (' ' == c || '\t' == c)
						continue; // Skipped...
					throw new ConfigInvalidException("Bad entry delimiter");
				}
				break;
			}

			if (Character.isLetterOrDigit((char) c) || c == '-') {
				// From the git-config man page:
				// The variable names are case-insensitive and only
				// alphanumeric characters and - are allowed.
				name.append((char) c);
			} else if ('\n' == c) {
				in.reset();
				name.append((char) c);
				break;
			} else
				throw new ConfigInvalidException("Bad entry name: " + name);
		}
		return name.toString();
	}

	private static String readValue(final StringReader in, boolean quote,
			final int eol) throws ConfigInvalidException {
		final StringBuffer value = new StringBuffer();
		boolean space = false;
		for (;;) {
			int c = in.read();
			if (c < 0) {
				if (value.length() == 0)
					throw new ConfigInvalidException("Unexpected end of config file");
				break;
			}

			if ('\n' == c) {
				if (quote)
					throw new ConfigInvalidException("Newline in quotes not allowed");
				in.reset();
				break;
			}

			if (eol == c)
				break;

			if (!quote) {
				if (Character.isWhitespace((char) c)) {
					space = true;
					continue;
				}
				if (';' == c || '#' == c) {
					in.reset();
					break;
				}
			}

			if (space) {
				if (value.length() > 0)
					value.append(' ');
				space = false;
			}

			if ('\\' == c) {
				c = in.read();
				switch (c) {
				case -1:
					throw new ConfigInvalidException("End of file in escape");
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
					throw new ConfigInvalidException("Bad escape: " + ((char) c));
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

	/**
	 * The configuration file entry
	 */
	private static class Entry {
		/**
		 * The text content before entry
		 */
		String prefix;

		/**
		 * The section name for the entry
		 */
		String section;

		/**
		 * Subsection name
		 */
		String subsection;

		/**
		 * The key name
		 */
		String name;

		/**
		 * The value
		 */
		String value;

		/**
		 * The text content after entry
		 */
		String suffix;

		boolean match(final String aSection, final String aSubsection,
				final String aKey) {
			return eqIgnoreCase(section, aSection)
					&& eqSameCase(subsection, aSubsection)
					&& eqIgnoreCase(name, aKey);
		}

		private static boolean eqIgnoreCase(final String a, final String b) {
			if (a == null && b == null)
				return true;
			if (a == null || b == null)
				return false;
			return StringUtils.equalsIgnoreCase(a, b);
		}

		private static boolean eqSameCase(final String a, final String b) {
			if (a == null && b == null)
				return true;
			if (a == null || b == null)
				return false;
			return a.equals(b);
		}
	}

	private static class StringReader {
		private final char[] buf;

		private int pos;

		StringReader(final String in) {
			buf = in.toCharArray();
		}

		int read() {
			try {
				return buf[pos++];
			} catch (ArrayIndexOutOfBoundsException e) {
				pos = buf.length;
				return -1;
			}
		}

		void reset() {
			pos--;
		}
	}
}
