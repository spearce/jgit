/*
 * Copyright (C) 2008, Google Inc.
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

package org.spearce.jgit.revwalk;

import java.util.Date;

import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.RepositoryTestCase;
import org.spearce.jgit.lib.Tree;

/** Support for tests of the {@link RevWalk} class. */
public abstract class RevWalkTestCase extends RepositoryTestCase {
	protected ObjectWriter ow;

	protected ObjectId emptyTree;

	protected long nowTick;

	protected RevWalk rw;

	public void setUp() throws Exception {
		super.setUp();
		ow = new ObjectWriter(db);
		emptyTree = ow.writeTree(new Tree(db));
		nowTick = 1236977987000L;
		rw = new RevWalk(db);
	}

	protected void tick(final int secDelta) {
		nowTick += secDelta * 1000L;
	}

	protected ObjectId commit(final ObjectId... parents) throws Exception {
		return commit(1, parents);
	}

	protected ObjectId commit(final int secDelta, final ObjectId... parents)
			throws Exception {
		tick(secDelta);
		final Commit c = new Commit(db);
		c.setTreeId(emptyTree);
		c.setParentIds(parents);
		c.setAuthor(new PersonIdent(jauthor, new Date(nowTick)));
		c.setCommitter(new PersonIdent(jcommitter, new Date(nowTick)));
		c.setMessage("");
		return ow.writeCommit(c);
	}

	protected RevCommit parse(final ObjectId commitId) throws Exception {
		return rw.parseCommit(commitId);
	}

	protected void markStart(final ObjectId commitId) throws Exception {
		rw.markStart(parse(commitId));
	}

	protected void markUninteresting(final ObjectId commitId) throws Exception {
		rw.markUninteresting(parse(commitId));
	}

	protected void assertCommit(final ObjectId commitId, final RevCommit commit) {
		assertEquals(commitId.name(), commit != null ? commit.name() : null);
	}
}
