/*
 *  Copyright (C) 2007 Dave Watson <dwatson@mimvista.com>
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility class to add reflog entries
 * 
 * @author Dave Watson
 */
public class RefLogWriter {
	/**
	 * Writes reflog entry for ref specified by refName
	 * 
	 * @param repo
	 *            repository to use
	 * @param oldCommit
	 *            previous commit
	 * @param commit
	 *            new commit
	 * @param message
	 *            reflog message
	 * @param refName
	 *            full ref name
	 * @throws IOException
	 */
	public static void writeReflog(Repository repo, ObjectId oldCommit,
			ObjectId commit, String message, String refName) throws IOException {
		String entry = buildReflogString(repo, oldCommit, commit, message);

		File directory = repo.getDirectory();
		PrintWriter writer = new PrintWriter(new FileOutputStream(new File(
				directory, "logs/" + refName), true));
		writer.println(entry);
		writer.close();
	}

	private static String buildReflogString(Repository repo,
			ObjectId oldCommit, ObjectId commit, String message) {
		PersonIdent me = new PersonIdent(repo);
		String s = oldCommit.toString() + " " + commit.toString() + " "
				+ me.toExternalString() + "\t" + message;
		return s;
	}

}
