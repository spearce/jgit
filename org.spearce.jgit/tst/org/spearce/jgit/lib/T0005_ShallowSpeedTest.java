/*
 *  Copyright (C) 2006  Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

import junit.textui.TestRunner;

public class T0005_ShallowSpeedTest extends SpeedTestBase {

	protected void setUp() throws Exception {
		prepare(new String[] { "git", "rev-list", "365bbe0d0caaf2ba74d56556827babf0bc66965d" });
	}

	public void testShallowHistoryScan() throws IOException {
		long start = System.currentTimeMillis();
		Repository db = new Repository(new File(kernelrepo));
		Commit commit = db.mapCommit("365bbe0d0caaf2ba74d56556827babf0bc66965d");
		int n = 1;
		do {
			// System.out.println("commit="+commit.getCommitId());
			ObjectId[] parents = commit.getParentIds();
			if (parents.length > 0) {
				ObjectId parentId = parents[0];
				commit = db.mapCommit(parentId);
				commit.getCommitId().toString();
				++n;
			} else {
				commit = null;
			}
		} while (commit != null);
		assertEquals(12275, n);
		long stop = System.currentTimeMillis();
		long time = stop - start;
		System.out.println("native="+nativeTime);
		System.out.println("jgit="+time);
		// ~0.750s (hot cache), ok
		/*
native=1795
jgit=722
		 */
		// native git seems to run SLOWER than jgit here, at roughly half the speed
		// creating the git process is not the issue here, btw.
		long factor10 = (nativeTime*150/time+50)/100;
		assertEquals(3, factor10);
	}

	public static void main(String[] args) {
		TestRunner.run(T0005_ShallowSpeedTest.class);
	}
}
