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
package org.spearce.jgit.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;

class FS_Win32_Cygwin extends FS_Win32 {
	private static String cygpath;

	static boolean detect() {
		final String path = AccessController
				.doPrivileged(new PrivilegedAction<String>() {
					public String run() {
						return System.getProperty("java.library.path");
					}
				});
		if (path == null)
			return false;
		for (final String p : path.split(";")) {
			final File e = new File(p, "cygpath.exe");
			if (e.isFile()) {
				cygpath = e.getAbsolutePath();
				return true;
			}
		}
		return false;
	}

	protected File resolveImpl(final File dir, final String pn) {
		try {
			final Process p;

			p = Runtime.getRuntime().exec(
					new String[] { cygpath, "--windows", "--absolute", pn },
					null, dir);
			p.getOutputStream().close();

			final BufferedReader lineRead = new BufferedReader(
					new InputStreamReader(p.getInputStream(), "UTF-8"));
			String r = null;
			try {
				r = lineRead.readLine();
			} finally {
				lineRead.close();
			}

			for (;;) {
				try {
					if (p.waitFor() == 0 && r != null && r.length() > 0)
						return new File(r);
					break;
				} catch (InterruptedException ie) {
					// Stop bothering me, I have a zombie to reap.
				}
			}
		} catch (IOException ioe) {
			// Fall through and use the default return.
			//
		}
		return super.resolveImpl(dir, pn);
	}
}
