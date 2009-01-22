/*
 * Copyright (C) 2008, Robin Rosenberg
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
package org.spearce.jgit.merge;

import java.io.IOException;

import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.RepositoryTestCase;

public class SimpleMergeTest extends RepositoryTestCase {

	public void testOurs() throws IOException {
		Merger ourMerger = MergeStrategy.OURS.newMerger(db);
		boolean merge = ourMerger.merge(new ObjectId[] { db.resolve("a"), db.resolve("c") });
		assertTrue(merge);
		assertEquals(db.mapTree("a").getId(), ourMerger.getResultTreeId());
	}

	public void testTheirs() throws IOException {
		Merger ourMerger = MergeStrategy.THEIRS.newMerger(db);
		boolean merge = ourMerger.merge(new ObjectId[] { db.resolve("a"), db.resolve("c") });
		assertTrue(merge);
		assertEquals(db.mapTree("c").getId(), ourMerger.getResultTreeId());
	}

	public void testTrivialTwoWay() throws IOException {
		Merger ourMerger = MergeStrategy.SIMPLE_TWO_WAY_IN_CORE.newMerger(db);
		boolean merge = ourMerger.merge(new ObjectId[] { db.resolve("a"), db.resolve("c") });
		assertTrue(merge);
		assertEquals("02ba32d3649e510002c21651936b7077aa75ffa9",ourMerger.getResultTreeId().name());
	}

	public void testTrivialTwoWay_disjointhistories() throws IOException {
		Merger ourMerger = MergeStrategy.SIMPLE_TWO_WAY_IN_CORE.newMerger(db);
		boolean merge = ourMerger.merge(new ObjectId[] { db.resolve("a"), db.resolve("c~4") });
		assertTrue(merge);
		assertEquals("86265c33b19b2be71bdd7b8cb95823f2743d03a8",ourMerger.getResultTreeId().name());
	}

	public void testTrivialTwoWay_ok() throws IOException {
		Merger ourMerger = MergeStrategy.SIMPLE_TWO_WAY_IN_CORE.newMerger(db);
		boolean merge = ourMerger.merge(new ObjectId[] { db.resolve("a^0^0^0"), db.resolve("a^0^0^1") });
		assertTrue(merge);
		assertEquals(db.mapTree("a^0^0").getId(), ourMerger.getResultTreeId());
	}

	public void testTrivialTwoWay_conflict() throws IOException {
		Merger ourMerger = MergeStrategy.SIMPLE_TWO_WAY_IN_CORE.newMerger(db);
		boolean merge = ourMerger.merge(new ObjectId[] { db.resolve("f"), db.resolve("g") });
		assertFalse(merge);
	}
}
