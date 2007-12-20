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
package org.spearce.jgit.pgm;

import java.io.File;
import java.io.IOException;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;

/**
 * Simple command line utility to show a log.
 */
public class Log {
	/**
	 * List all commits reachable from a root set
	 * @param args the refs or commits to start from
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Repository db = new Repository(new File(".git"));
		Commit commit = db.mapCommit(args[0]);
		System.out.println("commit " + commit.getCommitId());
		System.out.println("tree " + commit.getTreeId());
		ObjectId[] ps=commit.getParentIds();
		for (int ci=0; ci<ps.length; ++ci) {
			System.out.println("parent " + ps[ci]);
		}
		System.out.println("author " + commit.getAuthor());
		System.out.println();
		System.out.println(commit.getMessage());
	}
}
