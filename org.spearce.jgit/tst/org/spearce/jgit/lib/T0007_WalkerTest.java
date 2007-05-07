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
import java.util.Collection;

import junit.textui.TestRunner;

/**
 * A performance test like T0006_DeepSpeedTest, but more
 * realistic since it is smarter.
 */
public class T0007_WalkerTest extends SpeedTestBase {

	protected void setUp() throws Exception {
		prepare(new String[] { "git", "log", "365bbe0d0caaf2ba74d56556827babf0bc66965d","--","net/netfilter/nf_queue.c" });
	}

	public void testHistoryScan() throws IOException {
//		long start = System.currentTimeMillis();
		Repository db = new Repository(new File(kernelrepo));
		String[] path = { "net", "netfilter", "nf_queue.c" };
		Walker walker = new Walker(db,db.mapCommit(new ObjectId("365bbe0d0caaf2ba74d56556827babf0bc66965d")),path,true,true,null) {

			protected void collect(Collection ret, Commit commit, int count) {
				System.out.println("Got: "+count+" "+commit.getCommitId());
				ret.add(commit);
			}
		
		};
		Commit[] history = (Commit[])walker.collectHistory().toArray(new Commit[0]);
		assertEquals(8, history.length);
		assertEquals("365bbe0d0caaf2ba74d56556827babf0bc66965d",history[0].getCommitId().toString());
		assertEquals("a4c12d6c5dde48c69464baf7c703e425ee511433",history[1].getCommitId().toString());
		assertEquals("761a126017e3f001d3f5a574787aa232a9cd5bb5",history[2].getCommitId().toString());
		assertEquals("22a3e233ca08a2ddc949ba1ae8f6e16ec7ef1a13",history[3].getCommitId().toString());
		assertEquals("460fbf82c0842cad3f3c744c4dcb81978b7829f3",history[4].getCommitId().toString());
		assertEquals("272a5322d5219b00a1e541ad9d0d76824df1aa2a",history[5].getCommitId().toString());
		assertEquals("8e33ba49765484bc6de3a2f8143733713fa93bc1",history[6].getCommitId().toString());
		assertEquals("826509f8110049663799bc20f2b5b6170e2f78ca",history[7].getCommitId().toString());
		
	}

	public static void main(String[] args) {
		TestRunner.run(T0007_WalkerTest.class);
	}
}
