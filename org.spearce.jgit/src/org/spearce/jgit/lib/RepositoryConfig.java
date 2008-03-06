/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
		return new RepositoryConfig(null, new File(System
				.getProperty("user.home"), ".gitconfig"));
	}

	private final RepositoryConfig baseConfig;

	private final File configFile;

	private boolean readFile;

	private CoreConfig core;

	private List<Entry> entries;

	private Map<String, Object> byName;

	private Map<String, Entry> lastInEntry;

	private Map<String, Entry> lastInGroup;
	
	private static final String MAGIC_EMPTY_VALUE = "%%magic%%empty%%";

	RepositoryConfig(final Repository repo) {
		this(openUserConfig(), new File(repo.getDirectory(), "config"));
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
	 * @param section
	 * @param subsection
	 * @param name
	 * @param defaultValue
	 * @return an integer value from the git config
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
	 * @param subsection
	 * @param name
	 * @param defaultValue
	 * @return true if any value or defaultValue is true, false for missing or
	 *         explicit false
	 */
	protected boolean getBoolean(final String section, String subsection,
			final String name, final boolean defaultValue) {
		String n = getRawString(section, subsection, name);
		if (n == null)
			return defaultValue;

		n = n.toLowerCase();
		if (MAGIC_EMPTY_VALUE.equals(n) || "yes".equals(n) || "true".equals(n) || "1".equals(n)) {
			return true;
		} else if ("no".equals(n) || "false".equals(n) || "0".equals(n)) {
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
	
	private String getRawString(final String section, final String subsection,
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
		if (o instanceof List) {
			return ((Entry) ((List) o).get(0)).value;
		} else if (o instanceof Entry) {
			return ((Entry) o).value;
		} else if (baseConfig != null)
			return baseConfig.getRawString(section, subsection, name);
		else
			return null;
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
						Constants.CHARACTER_ENCODING)));
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
		} finally {
			r.close();
			if (!ok || !tmp.renameTo(configFile)) {
				tmp.delete();
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
				new FileInputStream(configFile), Constants.CHARACTER_ENCODING));
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
	}

	private void clear() {
		entries = new ArrayList<Entry>();
		byName = new HashMap<String, Object>();
		lastInEntry = new HashMap<String, Entry>();
		lastInGroup = new HashMap<String, Entry>();
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
				lastInEntry.put(key, e);
			}
			lastInGroup.put(group, e);
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
				throw new IOException("Unexpected end of config file.");
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
	}
}
