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
import java.util.List;

import junit.textui.TestRunner;

public class T0006_DeepSpeedTest extends SpeedTestBase {

	protected void setUp() throws Exception {
		prepare(new String[] { "git", "rev-list", "365bbe0d0caaf2ba74d56556827babf0bc66965d","--","net/netfilter/nf_queue.c" });
	}

	public void testDeepHistoryScan() throws IOException {
		long start = System.currentTimeMillis();
		Repository db = new Repository(new File(kernelrepo));
		Commit commit = db.mapCommit("365bbe0d0caaf2ba74d56556827babf0bc66965d");
		int n = 1;
		do {
			// System.out.println("commit="+commit.getCommitId());
			List parent = commit.getParentIds();
			if (parent.size() > 0) {
				ObjectId parentId = (ObjectId) parent.get(0);
				commit = db.mapCommit(parentId);
				TreeEntry m = commit.getTree().findBlobMember("net/netfilter/nf_queue.c");
				if (m != null)
					commit.getCommitId().toString();
				++n;
			} else {
				commit = null;
			}
		} while (commit != null);
		//
		assertEquals(12275, n);
		long stop = System.currentTimeMillis();
		long time = stop - start;
		System.out.println("native="+nativeTime);
		System.out.println("jgit="+time);
		/*
		native=1355
		jgit=5449
		 */
		// This is not an exact factor, but we'd expect native git to perform this
		// about 4 times quicker. If for some reason we find jgit to be faster than
		// this the cause should be found and secured.
		long factor = (time*110/nativeTime+50)/100;
		assertEquals(4, factor);
	}

	public static void main(String[] args) {
		TestRunner.run(T0006_DeepSpeedTest.class);
	}
}
