/*
 * Copyright (C) 2008, Charles O'Farrell <charleso@charleso.org>
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.spearce.jgit.lib.RefUpdate.Result;
import org.spearce.jgit.revwalk.RevCommit;

public class RefUpdateTest extends RepositoryTestCase {

	private RefUpdate updateRef(final String name) throws IOException {
		final RefUpdate ref = db.updateRef(name);
		ref.setNewObjectId(db.resolve(Constants.HEAD));
		return ref;
	}

	private void delete(final RefUpdate ref, final Result expected)
			throws IOException {
		delete(ref, expected, true, true);
	}

	private void delete(final RefUpdate ref, final Result expected,
			final boolean exists, final boolean removed) throws IOException {
		assertEquals(exists, db.getAllRefs().containsKey(ref.getName()));
		assertEquals(expected, ref.delete());
		assertEquals(!removed, db.getAllRefs().containsKey(ref.getName()));
	}

	public void testNoCacheObjectIdSubclass() throws IOException {
		final String newRef = "refs/heads/abc";
		final RefUpdate ru = updateRef(newRef);
		final RevCommit newid = new RevCommit(ru.getNewObjectId()) {
			// empty
		};
		ru.setNewObjectId(newid);
		Result update = ru.update();
		assertEquals(Result.NEW, update);
		final Ref r = db.getAllRefs().get(newRef);
		assertNotNull(r);
		assertEquals(newRef, r.getName());
		assertNotNull(r.getObjectId());
		assertNotSame(newid, r.getObjectId());
		assertSame(ObjectId.class, r.getObjectId().getClass());
		assertEquals(newid.copy(), r.getObjectId());
	}

	public void testNewNamespaceConflictWithLoosePrefixNameExists()
			throws IOException {
		final String newRef = "refs/heads/z";
		final RefUpdate ru = updateRef(newRef);
		final RevCommit newid = new RevCommit(ru.getNewObjectId()) {
			// empty
		};
		ru.setNewObjectId(newid);
		Result update = ru.update();
		assertEquals(Result.NEW, update);
		// end setup
		final String newRef2 = "refs/heads/z/a";
		final RefUpdate ru2 = updateRef(newRef2);
		final RevCommit newid2 = new RevCommit(ru2.getNewObjectId()) {
			// empty
		};
		ru.setNewObjectId(newid2);
		Result update2 = ru2.update();
		assertEquals(Result.LOCK_FAILURE, update2);
	}

	public void testNewNamespaceConflictWithPackedPrefixNameExists()
			throws IOException {
		final String newRef = "refs/heads/master/x";
		final RefUpdate ru = updateRef(newRef);
		final RevCommit newid = new RevCommit(ru.getNewObjectId()) {
			// empty
		};
		ru.setNewObjectId(newid);
		Result update = ru.update();
		assertEquals(Result.LOCK_FAILURE, update);
	}

	public void testNewNamespaceConflictWithLoosePrefixOfExisting()
			throws IOException {
		final String newRef = "refs/heads/z/a";
		final RefUpdate ru = updateRef(newRef);
		final RevCommit newid = new RevCommit(ru.getNewObjectId()) {
			// empty
		};
		ru.setNewObjectId(newid);
		Result update = ru.update();
		assertEquals(Result.NEW, update);
		// end setup
		final String newRef2 = "refs/heads/z";
		final RefUpdate ru2 = updateRef(newRef2);
		final RevCommit newid2 = new RevCommit(ru2.getNewObjectId()) {
			// empty
		};
		ru.setNewObjectId(newid2);
		Result update2 = ru2.update();
		assertEquals(Result.LOCK_FAILURE, update2);
	}

	public void testNewNamespaceConflictWithPackedPrefixOfExisting()
			throws IOException {
		final String newRef = "refs/heads/prefix";
		final RefUpdate ru = updateRef(newRef);
		final RevCommit newid = new RevCommit(ru.getNewObjectId()) {
			// empty
		};
		ru.setNewObjectId(newid);
		Result update = ru.update();
		assertEquals(Result.LOCK_FAILURE, update);
	}

	/**
	 * Delete a ref that is pointed to by HEAD
	 *
	 * @throws IOException
	 */
	public void testDeleteHEADreferencedRef() throws IOException {
		ObjectId pid = db.resolve("refs/heads/master^");
		RefUpdate updateRef = db.updateRef("refs/heads/master");
		updateRef.setNewObjectId(pid);
		updateRef.setForceUpdate(true);
		Result update = updateRef.update();
		assertEquals(Result.FORCED, update); // internal

		RefUpdate updateRef2 = db.updateRef("refs/heads/master");
		Result delete = updateRef2.delete();
		assertEquals(Result.REJECTED_CURRENT_BRANCH, delete);
		assertEquals(pid, db.resolve("refs/heads/master"));
	}

	public void testLooseDelete() throws IOException {
		final String newRef = "refs/heads/abc";
		RefUpdate ref = updateRef(newRef);
		ref.update(); // create loose ref
		ref = updateRef(newRef); // refresh
		delete(ref, Result.NO_CHANGE);
	}

	public void testDeleteHead() throws IOException {
		final RefUpdate ref = updateRef(Constants.HEAD);
		delete(ref, Result.REJECTED_CURRENT_BRANCH, true, false);
	}

	/**
	 * Delete a loose ref and make sure the directory in refs is deleted too,
	 * and the reflog dir too
	 *
	 * @throws IOException
	 */
	public void testDeleteLooseAndItsDirectory() throws IOException {
		ObjectId pid = db.resolve("refs/heads/c^");
		RefUpdate updateRef = db.updateRef("refs/heads/z/c");
		updateRef.setNewObjectId(pid);
		updateRef.setForceUpdate(true);
		updateRef.setRefLogMessage("new test ref", false);
		Result update = updateRef.update();
		assertEquals(Result.NEW, update); // internal
		assertTrue(new File(db.getDirectory(), Constants.R_HEADS + "z")
				.exists());
		assertTrue(new File(db.getDirectory(), "logs/refs/heads/z").exists());

		// The real test here
		RefUpdate updateRef2 = db.updateRef("refs/heads/z/c");
		updateRef2.setForceUpdate(true);
		Result delete = updateRef2.delete();
		assertEquals(Result.FORCED, delete);
		assertNull(db.resolve("refs/heads/z/c"));
		assertFalse(new File(db.getDirectory(), Constants.R_HEADS + "z")
				.exists());
		assertFalse(new File(db.getDirectory(), "logs/refs/heads/z").exists());
	}

	public void testDeleteNotFound() throws IOException {
		final RefUpdate ref = updateRef("refs/heads/xyz");
		delete(ref, Result.NEW, false, true);
	}

	public void testDeleteFastForward() throws IOException {
		final RefUpdate ref = updateRef("refs/heads/a");
		delete(ref, Result.FAST_FORWARD);
	}

	public void testDeleteForce() throws IOException {
		final RefUpdate ref = db.updateRef("refs/heads/b");
		ref.setNewObjectId(db.resolve("refs/heads/a"));
		delete(ref, Result.REJECTED, true, false);
		ref.setForceUpdate(true);
		delete(ref, Result.FORCED);
	}

	public void testRefKeySameAsOrigName() {
		Map<String, Ref> allRefs = db.getAllRefs();
		for (Entry<String, Ref> e : allRefs.entrySet()) {
			assertEquals(e.getKey(), e.getValue().getOrigName());

		}
	}

	/**
	 * Try modify a ref forward, fast forward
	 *
	 * @throws IOException
	 */
	public void testUpdateRefForward() throws IOException {
		ObjectId ppid = db.resolve("refs/heads/master^");
		ObjectId pid = db.resolve("refs/heads/master");

		RefUpdate updateRef = db.updateRef("refs/heads/master");
		updateRef.setNewObjectId(ppid);
		updateRef.setForceUpdate(true);
		Result update = updateRef.update();
		assertEquals(Result.FORCED, update);
		assertEquals(ppid, db.resolve("refs/heads/master"));

		// real test
		RefUpdate updateRef2 = db.updateRef("refs/heads/master");
		updateRef2.setNewObjectId(pid);
		Result update2 = updateRef2.update();
		assertEquals(Result.FAST_FORWARD, update2);
		assertEquals(pid, db.resolve("refs/heads/master"));
	}

	/**
	 * Delete a ref that exists both as packed and loose. Make sure the ref
	 * cannot be resolved after delete.
	 *
	 * @throws IOException
	 */
	public void testDeleteLoosePacked() throws IOException {
		ObjectId pid = db.resolve("refs/heads/c^");
		RefUpdate updateRef = db.updateRef("refs/heads/c");
		updateRef.setNewObjectId(pid);
		updateRef.setForceUpdate(true);
		Result update = updateRef.update();
		assertEquals(Result.FORCED, update); // internal

		// The real test here
		RefUpdate updateRef2 = db.updateRef("refs/heads/c");
		updateRef2.setForceUpdate(true);
		Result delete = updateRef2.delete();
		assertEquals(Result.FORCED, delete);
		assertNull(db.resolve("refs/heads/c"));
	}

	/**
	 * Try modify a ref to same
	 *
	 * @throws IOException
	 */
	public void testUpdateRefNoChange() throws IOException {
		ObjectId pid = db.resolve("refs/heads/master");
		RefUpdate updateRef = db.updateRef("refs/heads/master");
		updateRef.setNewObjectId(pid);
		Result update = updateRef.update();
		assertEquals(Result.NO_CHANGE, update);
		assertEquals(pid, db.resolve("refs/heads/master"));
	}

	/**
	 * Try modify a ref, but get wrong expected old value
	 *
	 * @throws IOException
	 */
	public void testUpdateRefLockFailureWrongOldValue() throws IOException {
		ObjectId pid = db.resolve("refs/heads/master");
		RefUpdate updateRef = db.updateRef("refs/heads/master");
		updateRef.setNewObjectId(pid);
		updateRef.setExpectedOldObjectId(db.resolve("refs/heads/master^"));
		Result update = updateRef.update();
		assertEquals(Result.LOCK_FAILURE, update);
		assertEquals(pid, db.resolve("refs/heads/master"));
	}

	/**
	 * Try modify a ref that is locked
	 *
	 * @throws IOException
	 */
	public void testUpdateRefLockFailureLocked() throws IOException {
		ObjectId opid = db.resolve("refs/heads/master");
		ObjectId pid = db.resolve("refs/heads/master^");
		RefUpdate updateRef = db.updateRef("refs/heads/master");
		updateRef.setNewObjectId(pid);
		LockFile lockFile1 = new LockFile(new File(db.getDirectory(),"refs/heads/master"));
		try {
			assertTrue(lockFile1.lock()); // precondition to test
			Result update = updateRef.update();
			assertEquals(Result.LOCK_FAILURE, update);
			assertEquals(opid, db.resolve("refs/heads/master"));
			LockFile lockFile2 = new LockFile(new File(db.getDirectory(),"refs/heads/master"));
			assertFalse(lockFile2.lock()); // was locked, still is
		} finally {
			lockFile1.unlock();
		}
	}

	/**
	 * Try to delete a ref. Delete requires force.
	 *
	 * @throws IOException
	 */
	public void testDeleteLoosePackedRejected() throws IOException {
		ObjectId pid = db.resolve("refs/heads/c^");
		ObjectId oldpid = db.resolve("refs/heads/c");
		RefUpdate updateRef = db.updateRef("refs/heads/c");
		updateRef.setNewObjectId(pid);
		Result update = updateRef.update();
		assertEquals(Result.REJECTED, update);
		assertEquals(oldpid, db.resolve("refs/heads/c"));
	}

	public void testRenameBranchNoPreviousLog() throws IOException {
		assertFalse("precondition, no log on old branchg", new File(db
				.getDirectory(), "logs/refs/heads/b").exists());
		ObjectId rb = db.resolve("refs/heads/b");
		ObjectId oldHead = db.resolve(Constants.HEAD);
		assertFalse(rb.equals(oldHead)); // assumption for this test
		RefRename renameRef = db.renameRef("refs/heads/b",
				"refs/heads/new/name");
		Result result = renameRef.rename();
		assertEquals(Result.RENAMED, result);
		assertEquals(rb, db.resolve("refs/heads/new/name"));
		assertNull(db.resolve("refs/heads/b"));
		assertEquals(1, db.getReflogReader("new/name").getReverseEntries().size());
		assertEquals("Branch: renamed b to new/name", db.getReflogReader("new/name")
				.getLastEntry().getComment());
		assertFalse(new File(db.getDirectory(), "logs/refs/heads/b").exists());
		assertEquals(oldHead, db.resolve(Constants.HEAD)); // unchanged
	}

	public void testRenameBranchHasPreviousLog() throws IOException {
		ObjectId rb = db.resolve("refs/heads/b");
		ObjectId oldHead = db.resolve(Constants.HEAD);
		assertFalse("precondition for this test, branch b != HEAD", rb
				.equals(oldHead));
		RefLogWriter.writeReflog(db, rb, rb, "Just a message", "refs/heads/b");
		assertTrue("no log on old branch", new File(db.getDirectory(),
				"logs/refs/heads/b").exists());
		RefRename renameRef = db.renameRef("refs/heads/b",
				"refs/heads/new/name");
		Result result = renameRef.rename();
		assertEquals(Result.RENAMED, result);
		assertEquals(rb, db.resolve("refs/heads/new/name"));
		assertNull(db.resolve("refs/heads/b"));
		assertEquals(2, db.getReflogReader("new/name").getReverseEntries().size());
		assertEquals("Branch: renamed b to new/name", db.getReflogReader("new/name")
				.getLastEntry().getComment());
		assertEquals("Just a message", db.getReflogReader("new/name")
				.getReverseEntries().get(1).getComment());
		assertFalse(new File(db.getDirectory(), "logs/refs/heads/b").exists());
		assertEquals(oldHead, db.resolve(Constants.HEAD)); // unchanged
	}

	public void testRenameCurrentBranch() throws IOException {
		ObjectId rb = db.resolve("refs/heads/b");
		db.writeSymref(Constants.HEAD, "refs/heads/b");
		ObjectId oldHead = db.resolve(Constants.HEAD);
		assertTrue("internal test condition, b == HEAD", rb.equals(oldHead));
		RefLogWriter.writeReflog(db, rb, rb, "Just a message", "refs/heads/b");
		assertTrue("no log on old branch", new File(db.getDirectory(),
				"logs/refs/heads/b").exists());
		RefRename renameRef = db.renameRef("refs/heads/b",
				"refs/heads/new/name");
		Result result = renameRef.rename();
		assertEquals(Result.RENAMED, result);
		assertEquals(rb, db.resolve("refs/heads/new/name"));
		assertNull(db.resolve("refs/heads/b"));
		assertEquals("Branch: renamed b to new/name", db.getReflogReader(
				"new/name").getLastEntry().getComment());
		assertFalse(new File(db.getDirectory(), "logs/refs/heads/b").exists());
		assertEquals(rb, db.resolve(Constants.HEAD));
		assertEquals(2, db.getReflogReader("new/name").getReverseEntries().size());
		assertEquals("Branch: renamed b to new/name", db.getReflogReader("new/name").getReverseEntries().get(0).getComment());
		assertEquals("Just a message", db.getReflogReader("new/name").getReverseEntries().get(1).getComment());
	}

	public void testRenameBranchAlsoInPack() throws IOException {
		ObjectId rb = db.resolve("refs/heads/b");
		ObjectId rb2 = db.resolve("refs/heads/b~1");
		assertEquals(Ref.Storage.PACKED, db.getRef("refs/heads/b").getStorage());
		RefUpdate updateRef = db.updateRef("refs/heads/b");
		updateRef.setNewObjectId(rb2);
		updateRef.setForceUpdate(true);
		Result update = updateRef.update();
		assertEquals("internal check new ref is loose", Result.FORCED, update);
		assertEquals(Ref.Storage.LOOSE_PACKED, db.getRef("refs/heads/b")
				.getStorage());
		RefLogWriter.writeReflog(db, rb, rb, "Just a message", "refs/heads/b");
		assertTrue("no log on old branch", new File(db.getDirectory(),
				"logs/refs/heads/b").exists());
		RefRename renameRef = db.renameRef("refs/heads/b",
				"refs/heads/new/name");
		Result result = renameRef.rename();
		assertEquals(Result.RENAMED, result);
		assertEquals(rb2, db.resolve("refs/heads/new/name"));
		assertNull(db.resolve("refs/heads/b"));
		assertEquals("Branch: renamed b to new/name", db.getReflogReader(
				"new/name").getLastEntry().getComment());
		assertFalse(new File(db.getDirectory(), "logs/refs/heads/b").exists());

		// Create new Repository instance, to reread caches and make sure our
		// assumptions are persistent.
		Repository ndb = new Repository(db.getDirectory());
		assertEquals(rb2, ndb.resolve("refs/heads/new/name"));
		assertNull(ndb.resolve("refs/heads/b"));
	}

	public void tryRenameWhenLocked(String toLock, String fromName,
			String toName, String headPointsTo) throws IOException {
		// setup
		db.writeSymref(Constants.HEAD, headPointsTo);
		ObjectId oldfromId = db.resolve(fromName);
		ObjectId oldHeadId = db.resolve(Constants.HEAD);
		RefLogWriter.writeReflog(db, oldfromId, oldfromId, "Just a message",
				fromName);
		List<org.spearce.jgit.lib.ReflogReader.Entry> oldFromLog = db
				.getReflogReader(fromName).getReverseEntries();
		List<org.spearce.jgit.lib.ReflogReader.Entry> oldHeadLog = oldHeadId != null ? db
				.getReflogReader(Constants.HEAD).getReverseEntries() : null;

		assertTrue("internal check, we have a log", new File(db.getDirectory(),
				"logs/" + fromName).exists());

		// "someone" has branch X locked
		LockFile lockFile = new LockFile(new File(db.getDirectory(), toLock));
		try {
			assertTrue(lockFile.lock());

			// Now this is our test
			RefRename renameRef = db.renameRef(fromName, toName);
			Result result = renameRef.rename();
			assertEquals(Result.LOCK_FAILURE, result);

			// Check that the involved refs are the same despite the failure
			assertExists(false, toName);
			if (!toLock.equals(toName))
				assertExists(false, toName + ".lock");
			assertExists(true, toLock + ".lock");
			if (!toLock.equals(fromName))
				assertExists(false, "logs/" + fromName + ".lock");
			assertExists(false, "logs/" + toName + ".lock");
			assertEquals(oldHeadId, db.resolve(Constants.HEAD));
			assertEquals(oldfromId, db.resolve(fromName));
			assertNull(db.resolve(toName));
			assertEquals(oldFromLog.toString(), db.getReflogReader(fromName)
					.getReverseEntries().toString());
			if (oldHeadId != null)
				assertEquals(oldHeadLog, db.getReflogReader(Constants.HEAD)
						.getReverseEntries());
		} finally {
			lockFile.unlock();
		}
	}

	private void assertExists(boolean positive, String toName) {
		assertEquals(toName + (positive ? " " : " does not ") + "exist",
				positive, new File(db.getDirectory(), toName).exists());
	}

	public void testRenameBranchCannotLockAFileHEADisFromLockHEAD()
			throws IOException {
		tryRenameWhenLocked("HEAD", "refs/heads/b", "refs/heads/new/name",
				"refs/heads/b");
	}

	public void testRenameBranchCannotLockAFileHEADisFromLockFrom()
			throws IOException {
		tryRenameWhenLocked("refs/heads/b", "refs/heads/b",
				"refs/heads/new/name", "refs/heads/b");
	}

	public void testRenameBranchCannotLockAFileHEADisFromLockTo()
			throws IOException {
		tryRenameWhenLocked("refs/heads/new/name", "refs/heads/b",
				"refs/heads/new/name", "refs/heads/b");
	}

	public void testRenameBranchCannotLockAFileHEADisToLockFrom()
			throws IOException {
		tryRenameWhenLocked("refs/heads/b", "refs/heads/b",
				"refs/heads/new/name", "refs/heads/new/name");
	}

	public void testRenameBranchCannotLockAFileHEADisToLockTo()
			throws IOException {
		tryRenameWhenLocked("refs/heads/new/name", "refs/heads/b",
				"refs/heads/new/name", "refs/heads/new/name");
	}

	public void testRenameBranchCannotLockAFileHEADisToLockTmp()
			throws IOException {
		tryRenameWhenLocked("RENAMED-REF.." + Thread.currentThread().getId(),
				"refs/heads/b", "refs/heads/new/name", "refs/heads/new/name");
	}

	public void testRenameBranchCannotLockAFileHEADisOtherLockFrom()
			throws IOException {
		tryRenameWhenLocked("refs/heads/b", "refs/heads/b",
				"refs/heads/new/name", "refs/heads/a");
	}

	public void testRenameBranchCannotLockAFileHEADisOtherLockTo()
			throws IOException {
		tryRenameWhenLocked("refs/heads/new/name", "refs/heads/b",
				"refs/heads/new/name", "refs/heads/a");
	}

	public void testRenameBranchCannotLockAFileHEADisOtherLockTmp()
			throws IOException {
		tryRenameWhenLocked("RENAMED-REF.." + Thread.currentThread().getId(),
				"refs/heads/b", "refs/heads/new/name", "refs/heads/a");
	}

	public void testRenameRefNameColission1avoided() throws IOException {
		// setup
		ObjectId rb = db.resolve("refs/heads/b");
		db.writeSymref(Constants.HEAD, "refs/heads/a");
		RefUpdate updateRef = db.updateRef("refs/heads/a");
		updateRef.setNewObjectId(rb);
		updateRef.setRefLogMessage("Setup", false);
		assertEquals(Result.FAST_FORWARD, updateRef.update());
		ObjectId oldHead = db.resolve(Constants.HEAD);
		assertTrue(rb.equals(oldHead)); // assumption for this test
		RefLogWriter.writeReflog(db, rb, rb, "Just a message", "refs/heads/a");
		assertTrue("internal check, we have a log", new File(db.getDirectory(),
				"logs/refs/heads/a").exists());

		// Now this is our test
		RefRename renameRef = db.renameRef("refs/heads/a", "refs/heads/a/b");
		Result result = renameRef.rename();
		assertEquals(Result.RENAMED, result);
		assertNull(db.resolve("refs/heads/a"));
		assertEquals(rb, db.resolve("refs/heads/a/b"));
		assertEquals(3, db.getReflogReader("a/b").getReverseEntries().size());
		assertEquals("Branch: renamed a to a/b", db.getReflogReader("a/b")
				.getReverseEntries().get(0).getComment());
		assertEquals("Just a message", db.getReflogReader("a/b")
				.getReverseEntries().get(1).getComment());
		assertEquals("Setup", db.getReflogReader("a/b").getReverseEntries()
				.get(2).getComment());
	}

	public void testRenameRefNameColission2avoided() throws IOException {
		// setup
		ObjectId rb = db.resolve("refs/heads/b");
		db.writeSymref(Constants.HEAD, "refs/heads/prefix/a");
		RefUpdate updateRef = db.updateRef("refs/heads/prefix/a");
		updateRef.setNewObjectId(rb);
		updateRef.setRefLogMessage("Setup", false);
		updateRef.setForceUpdate(true);
		assertEquals(Result.FORCED, updateRef.update());
		ObjectId oldHead = db.resolve(Constants.HEAD);
		assertTrue(rb.equals(oldHead)); // assumption for this test
		RefLogWriter.writeReflog(db, rb, rb, "Just a message",
				"refs/heads/prefix/a");
		assertTrue("internal check, we have a log", new File(db.getDirectory(),
				"logs/refs/heads/prefix/a").exists());

		// Now this is our test
		RefRename renameRef = db.renameRef("refs/heads/prefix/a",
				"refs/heads/prefix");
		Result result = renameRef.rename();
		assertEquals(Result.RENAMED, result);

		assertNull(db.resolve("refs/heads/prefix/a"));
		assertEquals(rb, db.resolve("refs/heads/prefix"));
		assertEquals(3, db.getReflogReader("prefix").getReverseEntries().size());
		assertEquals("Branch: renamed prefix/a to prefix", db.getReflogReader(
				"prefix").getReverseEntries().get(0).getComment());
		assertEquals("Just a message", db.getReflogReader("prefix")
				.getReverseEntries().get(1).getComment());
		assertEquals("Setup", db.getReflogReader("prefix").getReverseEntries()
				.get(2).getComment());
	}
}
