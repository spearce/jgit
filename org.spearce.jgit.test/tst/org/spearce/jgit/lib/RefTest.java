/*
 * Copyright (C) 2009, Robin Rosenberg
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

import java.util.Map;

/**
 * Misc tests for refs. A lot of things are tested elsewhere so not having a
 * test for a ref related method, does not mean it is untested.
 */
public class RefTest extends RepositoryTestCase {

	public void testReadAllIncludingSymrefs() throws Exception {
		ObjectId masterId = db.resolve("refs/heads/master");
		RefUpdate updateRef = db.updateRef("refs/remotes/origin/master");
		updateRef.setNewObjectId(masterId);
		updateRef.setForceUpdate(true);
		updateRef.update();
		db
				.writeSymref("refs/remotes/origin/HEAD",
						"refs/remotes/origin/master");

		ObjectId r = db.resolve("refs/remotes/origin/HEAD");
		assertEquals(masterId, r);

		Map<String, Ref> allRefs = db.getAllRefs();
		Ref refHEAD = allRefs.get("refs/remotes/origin/HEAD");
		assertNotNull(refHEAD);
		assertEquals(masterId, refHEAD.getObjectId());
		assertTrue(refHEAD.isPeeled());
		assertNull(refHEAD.getPeeledObjectId());

		Ref refmaster = allRefs.get("refs/remotes/origin/master");
		assertEquals(masterId, refmaster.getObjectId());
		assertFalse(refmaster.isPeeled());
		assertNull(refmaster.getPeeledObjectId());
	}
}
