/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.pgm;

import java.io.File;
import java.util.Arrays;

import org.spearce.jgit.lib.Repository;

/** Command line entry point. */
public class Main {
	/**
	 * Execute the command line.
	 * 
	 * @param argv
	 *            arguments.
	 */
	public static void main(final String[] argv) {
		try {
			execute(argv);
		} catch (Die err) {
			System.err.println("fatal: " + err.getMessage());
			System.exit(128);
		} catch (Exception err) {
			if (err.getClass().getName().startsWith("org.spearce.jgit.errors.")) {
				System.err.println("fatal: " + err.getMessage());
				System.exit(128);
			}
			err.printStackTrace();
			System.exit(1);
		}
	}

	private static void execute(final String[] argv) throws Exception {
		int argi = 0;
		String gitdir = ".git";

		if (argi == argv.length)
			usage();
		if (argv[argi].startsWith("--git-dir="))
			gitdir = argv[argi++].substring("--git-dir=".length());

		if (argi == argv.length)
			usage();
		final TextBuiltin cmd = createCommand(argv[argi++]);
		cmd.db = new Repository(new File(gitdir));
		try {
			cmd.execute(subarray(argv, argi));
		} finally {
			if (cmd.out != null)
				cmd.out.flush();
		}
	}

	private static String[] subarray(final String[] argv, final int i) {
		return Arrays.asList(argv).subList(i, argv.length).toArray(
				new String[0]);
	}

	private static TextBuiltin createCommand(final String name) {
		final StringBuilder s = new StringBuilder();
		s.append(mypackage());
		s.append('.');
		boolean upnext = true;
		for (int i = 0; i < name.length(); i++) {
			final char c = name.charAt(i);
			if (c == '-') {
				upnext = true;
				continue;
			}
			if (upnext)
				s.append(Character.toUpperCase(c));
			else
				s.append(c);
			upnext = false;
		}
		try {
			return (TextBuiltin) Class.forName(s.toString()).newInstance();
		} catch (Exception e) {
			System.err.println("error: " + name + " is not a jgit command.");
			System.exit(1);
			return null;
		}
	}

	private static String mypackage() {
		final String p = Main.class.getName();
		final int dot = p.lastIndexOf('.');
		return p.substring(0, dot);
	}

	private static void usage() {
		System.err.println("jgit [--git-dir=path] cmd ...");
		System.exit(1);
	}
}
