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
		ru.update();
		final Ref r = db.getAllRefs().get(newRef);
		assertNotNull(r);
		assertEquals(newRef, r.getName());
		assertNotNull(r.getObjectId());
		assertNotSame(newid, r.getObjectId());
		assertSame(ObjectId.class, r.getObjectId().getClass());
		assertEquals(newid.copy(), r.getObjectId());
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

	public void testLogDeleted() throws IOException {
		String refName = "refs/heads/a";
		final File log = createLog(refName);
		assertTrue(log.exists());
		final RefUpdate ref = updateRef(refName);
		delete(ref, Result.FAST_FORWARD);
		assertFalse(log.exists());
	}

	private File createLog(String name) throws IOException {
		final File log = new File(db.getDirectory(), Constants.LOGS + "/"
				+ name);
		log.getParentFile().mkdirs();
		log.createNewFile();
		return log;
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

	public void testDeleteEmptyDirs() throws IOException {
		final String top = "refs/heads/a";
		final String newRef = top + "/b/c";
		final String newRef2 = top + "/d";
		updateRef(newRef).update();
		updateRef(newRef2).update();
		delete(updateRef(newRef2), Result.NO_CHANGE);
		assertExists(true, top);
		createLog(newRef);
		delete(updateRef(newRef), Result.NO_CHANGE);
		assertExists(false, top);
		assertExists(false, Constants.LOGS + "/" + top);
	}

	private void assertExists(final boolean expected, final String name) {
		assertEquals(expected, new File(db.getDirectory(), name).exists());
	}

	public void testRefKeySameAsOrigName() {
		Map<String, Ref> allRefs = db.getAllRefs();
		for (Entry<String, Ref> e : allRefs.entrySet()) {
			assertEquals(e.getKey(), e.getValue().getOrigName());

		}
	}
}
