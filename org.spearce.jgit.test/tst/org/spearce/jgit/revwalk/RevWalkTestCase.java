/*
 * Copyright (C) 2009, Google Inc.
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
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.RepositoryTestCase;
import org.spearce.jgit.lib.Tag;
import org.spearce.jgit.lib.Tree;

/** Support for tests of the {@link RevWalk} class. */
public abstract class RevWalkTestCase extends RepositoryTestCase {
	protected ObjectWriter ow;

	protected RevTree emptyTree;

	protected long nowTick;

	protected RevWalk rw;

	public void setUp() throws Exception {
		super.setUp();
		ow = new ObjectWriter(db);
		rw = new RevWalk(db);
		emptyTree = rw.parseTree(ow.writeTree(new Tree(db)));
		nowTick = 1236977987000L;
	}

	protected void tick(final int secDelta) {
		nowTick += secDelta * 1000L;
	}

	protected RevBlob blob(final String content) throws Exception {
		return rw.lookupBlob(ow.writeBlob(content
				.getBytes(Constants.CHARACTER_ENCODING)));
	}

	protected RevCommit commit(final RevCommit... parents) throws Exception {
		return commit(1, parents);
	}

	protected RevCommit commit(final int secDelta, final RevCommit... parents)
			throws Exception {
		tick(secDelta);
		final Commit c = new Commit(db);
		c.setTreeId(emptyTree);
		c.setParentIds(parents);
		c.setAuthor(new PersonIdent(jauthor, new Date(nowTick)));
		c.setCommitter(new PersonIdent(jcommitter, new Date(nowTick)));
		c.setMessage("");
		return rw.lookupCommit(ow.writeCommit(c));
	}

	protected RevTag tag(final String name, final RevObject dst)
			throws Exception {
		final Tag t = new Tag(db);
		t.setType(Constants.typeString(dst.getType()));
		t.setObjId(dst.toObjectId());
		t.setTag(name);
		t.setTagger(new PersonIdent(jcommitter, new Date(nowTick)));
		t.setMessage("");
		return (RevTag) rw.lookupAny(ow.writeTag(t), Constants.OBJ_TAG);
	}

	protected <T extends RevObject> T parse(final T t) throws Exception {
		rw.parse(t);
		return t;
	}

	protected void markStart(final RevCommit commit) throws Exception {
		rw.markStart(commit);
	}

	protected void markUninteresting(final RevCommit commit) throws Exception {
		rw.markUninteresting(commit);
	}

	protected void assertCommit(final RevCommit exp, final RevCommit act) {
		assertSame(exp, act);
	}
}
