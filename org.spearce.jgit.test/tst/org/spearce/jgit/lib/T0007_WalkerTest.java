/*
 *  Copyright (C) 2006, 2007  Robin Rosenberg
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

import java.io.IOException;

import junit.textui.TestRunner;

public class T0007_WalkerTest extends RepositoryTestCase {

	private static String[] parsePath(String path) {
		String[] ret = path.split("/");
		if (ret.length==1 && ret[0].equals(""))
			return new String[0];
		return ret;
	}

	Commit[] mapCommits(String[] name) throws IOException {
		Commit[] ret = new Commit[name.length];
		for (int i=0; i<name.length; ++i)
			ret[i] = db.mapCommit(name[i]);
		return ret;
	}
	
	class MyWalker extends TopologicalWalker {

		public MyWalker(String[] starts, String path, boolean leafIsBlob,boolean followMainOnly, Boolean merges, ObjectId activeDiffLeafId) throws IOException {
			super(db, mapCommits(starts), parsePath(path), leafIsBlob, followMainOnly, merges, activeDiffLeafId, false);
		}

		protected boolean isCancelled() {
			return false;
		}

		@Override
		protected void collect(Commit commit, int count, int breadth) {
			ObjectId commitId = commit.getCommitId();
			if (commitId == null)
				commitId = ObjectId.zeroId();
			super.collect(commit, count, breadth);
		}
		
		ObjectId[] collect() {
			return (ObjectId[]) collectHistory().toArray(new ObjectId[0]);
//			ObjectId[] r = (ObjectId[]) ret.toArray(new ObjectId[0]);
//			Arrays.sort(r);
//			return r;
		}
	};

	MyWalker newWalker(String[] starts, String path, boolean leafIsBlob,boolean followMainOnly, Boolean merges, ObjectId activeDiffLeafId) {
		try {
			return new MyWalker(starts,path,leafIsBlob,followMainOnly,merges,activeDiffLeafId);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	MyWalker newWalker(String start, String path, boolean leafIsBlob,boolean followMainOnly, Boolean merges, ObjectId activeDiffLeafId) {
		return newWalker(new String[] { start }, path, leafIsBlob, followMainOnly, merges, activeDiffLeafId);
	}

	/** These are simple case, but account for the <em>last</em> commit */
	static final String FIRST_COMMIT_ON_MASTER = "6c8b137b1c652731597c89668f417b8695f28dd7";

	/** We are using a path with no match */
	public void testHistoryScanLast_1_nomatch() {
		MyWalker walker = newWalker(FIRST_COMMIT_ON_MASTER,"master.x",true,true,null,null);
		assertEquals(0, walker.collect().length);
	}

	/** We only want merges, but the path is valid so we do not get any */
	public void testHistoryScanLast_2_onlymerges() {
		MyWalker walker = newWalker(FIRST_COMMIT_ON_MASTER,"master.txt",true,true,Boolean.TRUE,null);
		assertEquals(0, walker.collect().length);
	}

	/** We do not want merges, the path is valid so we get a null (which means that the commit itself
	 *  introduces a change, plus the commit which introduces a change against nothing.
	 */
	public void testHistoryScanLast_2_nomerges() {
		MyWalker walker = newWalker(FIRST_COMMIT_ON_MASTER,"master.txt",true,true,Boolean.FALSE,null);
		ObjectId[] h = walker.collect();
		assertEquals(1, h.length);
		assertEquals(FIRST_COMMIT_ON_MASTER, h[0].toString());
	}

	/** We accept merges and non-merges, the path is valid so we get a null (which means that the
	 *  commit itself introduces a change, plus the commit which introduces a change against nothing.
	 *  Since there are no merges here this means nothing on practice.
	 */
	public void testHistoryScanLast_2_any() {
		MyWalker walker = newWalker(FIRST_COMMIT_ON_MASTER,"master.txt",true,true,null,null);
		ObjectId[] h = walker.collect();
		assertEquals(1, h.length);
		assertEquals(FIRST_COMMIT_ON_MASTER, h[0].toString());
	}

	/** We add a reference to compare with. In this case the data does not match the blob so the state
	 *  after the commit is a change, as is the change introduced by the commit.
	 */
	public void testHistoryScanLast_2_any_with_change_from_reference() {
		MyWalker walker = newWalker(FIRST_COMMIT_ON_MASTER,"master.txt",true,true,null,ObjectId.fromString("82b1d08466e9505f8666b778744f9a3471a70c81"));
		ObjectId[] h = walker.collect();
		assertEquals(2, h.length);
		assertEquals("0000000000000000000000000000000000000000", h[0].toString());
		assertEquals(FIRST_COMMIT_ON_MASTER, h[1].toString());
	}

	/** We add a reference to compare with. In this case the reference SHA-1 matches the state in the commit
	 *  so the commit doesn't change anything, but it does relative to nothing, i.e. log the commit.
	 */
	public void testHistoryScanLast_2_any_with_no_change_from_reference() {
		MyWalker walker = newWalker(FIRST_COMMIT_ON_MASTER,"master.txt",true,true,null,ObjectId.fromString("8230f48330e0055d9e0bc5a2a77718f6dd9324b8"));
		ObjectId[] h = walker.collect();
		assertEquals(1,h.length);
		assertEquals(FIRST_COMMIT_ON_MASTER, h[0].toString());
	}

	/** Give the null id as a reference, which means the state of the commit vs "index" is a change as is
	 *  the change vs nothing.
	 */
	public void testHistoryScanLast_2_any_file_dropped() {
		MyWalker walker = newWalker(FIRST_COMMIT_ON_MASTER,"master.txt",true,true,null,ObjectId.zeroId());
		ObjectId[] h = walker.collect();
		assertEquals(2, h.length);
		assertEquals("0000000000000000000000000000000000000000", h[0].toString());
		assertEquals(FIRST_COMMIT_ON_MASTER, h[1].toString());
	}

	/** A simple first parent scan of all commits
	 */
	public void testSimpleScanFirstParentIncludingMerges() {
		MyWalker walker = newWalker("master^1^2" /* Second b/b2 */, "", false, true, null, null);
		ObjectId[] h = walker.collect();
		assertEquals(7, h.length);
		assertEquals("7f822839a2fe9760f386cbbbcb3f92c5fe81def7", h[0].toString());
		assertEquals("59706a11bde2b9899a278838ef20a97e8f8795d2", h[1].toString());
		assertEquals("c070ad8c08840c8116da865b2d65593a6bb9cd2a", h[2].toString());
		assertEquals("d31f5a60d406e831d056b8ac2538d515100c2df2", h[3].toString());
		assertEquals("83d2f0431bcdc9c2fd2c17b828143be6ee4fbe80", h[4].toString());
		assertEquals("58be4659bb571194ed4562d04b359d26216f526e", h[5].toString());
		assertEquals("6c8b137b1c652731597c89668f417b8695f28dd7", h[6].toString());
	}

	/** A simple first parent scan of all commits, no merges
	 */
	public void testSimpleScanFirstParentExcludingMerges() {
		MyWalker walker = newWalker("master^1^2" /* Second b/b2 */, "", false, true, Boolean.FALSE, null);
		ObjectId[] h = walker.collect();
		assertEquals(6, h.length);
		assertEquals("7f822839a2fe9760f386cbbbcb3f92c5fe81def7", h[0].toString());
		assertEquals("59706a11bde2b9899a278838ef20a97e8f8795d2", h[1].toString());
		assertEquals("d31f5a60d406e831d056b8ac2538d515100c2df2", h[2].toString());
		assertEquals("83d2f0431bcdc9c2fd2c17b828143be6ee4fbe80", h[3].toString());
		assertEquals("58be4659bb571194ed4562d04b359d26216f526e", h[4].toString());
		assertEquals("6c8b137b1c652731597c89668f417b8695f28dd7", h[5].toString());
	}

	/** A simple first parent scan of all commits, only merges
	 */
	public void testSimpleScanFirstParentOnlyMerges() {
		MyWalker walker = newWalker("master^1^2" /* Second b/b2 */, "", false, true, Boolean.TRUE, null);
		ObjectId[] h = walker.collect();
		assertEquals(1, h.length);
		assertEquals("c070ad8c08840c8116da865b2d65593a6bb9cd2a", h[0].toString());
	}

	/** Select all commits from a point, starting at a merge
	 */
	public void testSimpleScanMultiplePaths() {
		MyWalker walker = newWalker("a^^" /* 0966a434eb1a025db6b71485ab63a3bfbea520b6 */ , "", false, false, null, null);
		ObjectId[] h = walker.collect();
		assertEquals(5, h.length);
		assertEquals("0966a434eb1a025db6b71485ab63a3bfbea520b6", h[0].toString());
		assertEquals("2c349335b7f797072cf729c4f3bb0914ecb6dec9", h[1].toString());
		assertEquals("ac7e7e44c1885efb472ad54a78327d66bfc4ecef", h[2].toString());
		assertEquals("58be4659bb571194ed4562d04b359d26216f526e", h[3].toString());
		assertEquals("6c8b137b1c652731597c89668f417b8695f28dd7", h[4].toString());
	}

	/** Select all commits from a point, starting at a normal commit
	 */
	public void testSimpleScanMultiplePaths_2() {
		MyWalker walker = newWalker("a^" /* d86a2aada2f5e7ccf6f11880bfb9ab404e8a8864 */ , "", false, false, null, null);
		ObjectId[] h = walker.collect();
		assertEquals(6, h.length);
		assertEquals("d86a2aada2f5e7ccf6f11880bfb9ab404e8a8864", h[0].toString());
		assertEquals("0966a434eb1a025db6b71485ab63a3bfbea520b6", h[1].toString());
		assertEquals("2c349335b7f797072cf729c4f3bb0914ecb6dec9", h[2].toString());
		assertEquals("ac7e7e44c1885efb472ad54a78327d66bfc4ecef", h[3].toString());
		assertEquals("58be4659bb571194ed4562d04b359d26216f526e", h[4].toString());
		assertEquals("6c8b137b1c652731597c89668f417b8695f28dd7", h[5].toString());
	}

	public static void main(String[] args) {
		TestRunner.run(T0007_WalkerTest.class);
	}
}
