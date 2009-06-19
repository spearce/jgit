/*
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
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

package org.spearce.jgit.transport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.spearce.jgit.lib.RepositoryTestCase;

public class RemoteConfigTest extends RepositoryTestCase {
	private void writeConfig(final String dat) throws FileNotFoundException,
			IOException, UnsupportedEncodingException {
		final File file = new File(db.getDirectory(), "config");
		final FileOutputStream stream = new FileOutputStream(file, true);
		try {
			stream.write(dat.getBytes("UTF-8"));
		} finally {
			stream.close();
		}
		db.getConfig().load();
	}

	private static void assertEquals(final String exp, final URIish act) {
		assertEquals(exp, act != null ? act.toString() : null);
	}

	public void testSimple() throws Exception {
		writeConfig("[remote \"spearce\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "fetch = +refs/heads/*:refs/remotes/spearce/*\n");

		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "spearce");
		final List<URIish> allURIs = rc.getURIs();
		RefSpec spec;

		assertEquals("spearce", rc.getName());
		assertNotNull(allURIs);
		assertNotNull(rc.getFetchRefSpecs());
		assertNotNull(rc.getPushRefSpecs());
		assertNotNull(rc.getTagOpt());
		assertEquals(0, rc.getTimeout());
		assertSame(TagOpt.AUTO_FOLLOW, rc.getTagOpt());

		assertEquals(1, allURIs.size());
		assertEquals("http://www.spearce.org/egit.git", allURIs.get(0));

		assertEquals(1, rc.getFetchRefSpecs().size());
		spec = rc.getFetchRefSpecs().get(0);
		assertTrue(spec.isForceUpdate());
		assertTrue(spec.isWildcard());
		assertEquals("refs/heads/*", spec.getSource());
		assertEquals("refs/remotes/spearce/*", spec.getDestination());

		assertEquals(0, rc.getPushRefSpecs().size());
	}

	public void testSimpleNoTags() throws Exception {
		writeConfig("[remote \"spearce\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "fetch = +refs/heads/*:refs/remotes/spearce/*\n"
				+ "tagopt = --no-tags\n");
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "spearce");
		assertSame(TagOpt.NO_TAGS, rc.getTagOpt());
	}

	public void testSimpleAlwaysTags() throws Exception {
		writeConfig("[remote \"spearce\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "fetch = +refs/heads/*:refs/remotes/spearce/*\n"
				+ "tagopt = --tags\n");
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "spearce");
		assertSame(TagOpt.FETCH_TAGS, rc.getTagOpt());
	}

	public void testMirror() throws Exception {
		writeConfig("[remote \"spearce\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "fetch = +refs/heads/*:refs/heads/*\n"
				+ "fetch = refs/tags/*:refs/tags/*\n");

		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "spearce");
		final List<URIish> allURIs = rc.getURIs();
		RefSpec spec;

		assertEquals("spearce", rc.getName());
		assertNotNull(allURIs);
		assertNotNull(rc.getFetchRefSpecs());
		assertNotNull(rc.getPushRefSpecs());

		assertEquals(1, allURIs.size());
		assertEquals("http://www.spearce.org/egit.git", allURIs.get(0));

		assertEquals(2, rc.getFetchRefSpecs().size());

		spec = rc.getFetchRefSpecs().get(0);
		assertTrue(spec.isForceUpdate());
		assertTrue(spec.isWildcard());
		assertEquals("refs/heads/*", spec.getSource());
		assertEquals("refs/heads/*", spec.getDestination());

		spec = rc.getFetchRefSpecs().get(1);
		assertFalse(spec.isForceUpdate());
		assertTrue(spec.isWildcard());
		assertEquals("refs/tags/*", spec.getSource());
		assertEquals("refs/tags/*", spec.getDestination());

		assertEquals(0, rc.getPushRefSpecs().size());
	}

	public void testBackup() throws Exception {
		writeConfig("[remote \"backup\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "url = user@repo.or.cz:/srv/git/egit.git\n"
				+ "push = +refs/heads/*:refs/heads/*\n"
				+ "push = refs/tags/*:refs/tags/*\n");

		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "backup");
		final List<URIish> allURIs = rc.getURIs();
		RefSpec spec;

		assertEquals("backup", rc.getName());
		assertNotNull(allURIs);
		assertNotNull(rc.getFetchRefSpecs());
		assertNotNull(rc.getPushRefSpecs());

		assertEquals(2, allURIs.size());
		assertEquals("http://www.spearce.org/egit.git", allURIs.get(0));
		assertEquals("user@repo.or.cz:/srv/git/egit.git", allURIs.get(1));

		assertEquals(0, rc.getFetchRefSpecs().size());

		assertEquals(2, rc.getPushRefSpecs().size());
		spec = rc.getPushRefSpecs().get(0);
		assertTrue(spec.isForceUpdate());
		assertTrue(spec.isWildcard());
		assertEquals("refs/heads/*", spec.getSource());
		assertEquals("refs/heads/*", spec.getDestination());

		spec = rc.getPushRefSpecs().get(1);
		assertFalse(spec.isForceUpdate());
		assertTrue(spec.isWildcard());
		assertEquals("refs/tags/*", spec.getSource());
		assertEquals("refs/tags/*", spec.getDestination());
	}

	public void testUploadPack() throws Exception {
		writeConfig("[remote \"example\"]\n"
				+ "url = user@example.com:egit.git\n"
				+ "fetch = +refs/heads/*:refs/remotes/example/*\n"
				+ "uploadpack = /path/to/git/git-upload-pack\n"
				+ "receivepack = /path/to/git/git-receive-pack\n");

		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "example");
		final List<URIish> allURIs = rc.getURIs();
		RefSpec spec;

		assertEquals("example", rc.getName());
		assertNotNull(allURIs);
		assertNotNull(rc.getFetchRefSpecs());
		assertNotNull(rc.getPushRefSpecs());

		assertEquals(1, allURIs.size());
		assertEquals("user@example.com:egit.git", allURIs.get(0));

		assertEquals(1, rc.getFetchRefSpecs().size());
		spec = rc.getFetchRefSpecs().get(0);
		assertTrue(spec.isForceUpdate());
		assertTrue(spec.isWildcard());
		assertEquals("refs/heads/*", spec.getSource());
		assertEquals("refs/remotes/example/*", spec.getDestination());

		assertEquals(0, rc.getPushRefSpecs().size());

		assertEquals("/path/to/git/git-upload-pack", rc.getUploadPack());
		assertEquals("/path/to/git/git-receive-pack", rc.getReceivePack());
	}

	public void testUnknown() throws Exception {
		writeConfig("");

		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "backup");
		assertEquals(0, rc.getURIs().size());
		assertEquals(0, rc.getFetchRefSpecs().size());
		assertEquals(0, rc.getPushRefSpecs().size());
		assertEquals("git-upload-pack", rc.getUploadPack());
		assertEquals("git-receive-pack", rc.getReceivePack());
	}

	public void testAddURI() throws Exception {
		writeConfig("");

		final URIish uri = new URIish("/some/dir");
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "backup");
		assertEquals(0, rc.getURIs().size());

		assertTrue(rc.addURI(uri));
		assertEquals(1, rc.getURIs().size());
		assertSame(uri, rc.getURIs().get(0));

		assertFalse(rc.addURI(new URIish(uri.toString())));
		assertEquals(1, rc.getURIs().size());
	}

	public void testRemoveFirstURI() throws Exception {
		writeConfig("");

		final URIish a = new URIish("/some/dir");
		final URIish b = new URIish("/another/dir");
		final URIish c = new URIish("/more/dirs");
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "backup");
		assertTrue(rc.addURI(a));
		assertTrue(rc.addURI(b));
		assertTrue(rc.addURI(c));

		assertEquals(3, rc.getURIs().size());
		assertSame(a, rc.getURIs().get(0));
		assertSame(b, rc.getURIs().get(1));
		assertSame(c, rc.getURIs().get(2));

		assertTrue(rc.removeURI(a));
		assertEquals(2, rc.getURIs().size());
		assertSame(b, rc.getURIs().get(0));
		assertSame(c, rc.getURIs().get(1));
	}

	public void testRemoveMiddleURI() throws Exception {
		writeConfig("");

		final URIish a = new URIish("/some/dir");
		final URIish b = new URIish("/another/dir");
		final URIish c = new URIish("/more/dirs");
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "backup");
		assertTrue(rc.addURI(a));
		assertTrue(rc.addURI(b));
		assertTrue(rc.addURI(c));

		assertEquals(3, rc.getURIs().size());
		assertSame(a, rc.getURIs().get(0));
		assertSame(b, rc.getURIs().get(1));
		assertSame(c, rc.getURIs().get(2));

		assertTrue(rc.removeURI(b));
		assertEquals(2, rc.getURIs().size());
		assertSame(a, rc.getURIs().get(0));
		assertSame(c, rc.getURIs().get(1));
	}

	public void testRemoveLastURI() throws Exception {
		writeConfig("");

		final URIish a = new URIish("/some/dir");
		final URIish b = new URIish("/another/dir");
		final URIish c = new URIish("/more/dirs");
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "backup");
		assertTrue(rc.addURI(a));
		assertTrue(rc.addURI(b));
		assertTrue(rc.addURI(c));

		assertEquals(3, rc.getURIs().size());
		assertSame(a, rc.getURIs().get(0));
		assertSame(b, rc.getURIs().get(1));
		assertSame(c, rc.getURIs().get(2));

		assertTrue(rc.removeURI(c));
		assertEquals(2, rc.getURIs().size());
		assertSame(a, rc.getURIs().get(0));
		assertSame(b, rc.getURIs().get(1));
	}

	public void testRemoveOnlyURI() throws Exception {
		writeConfig("");

		final URIish a = new URIish("/some/dir");
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "backup");
		assertTrue(rc.addURI(a));

		assertEquals(1, rc.getURIs().size());
		assertSame(a, rc.getURIs().get(0));

		assertTrue(rc.removeURI(a));
		assertEquals(0, rc.getURIs().size());
	}

	public void testCreateOrigin() throws Exception {
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "origin");
		rc.addURI(new URIish("/some/dir"));
		rc.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/"
				+ rc.getName() + "/*"));
		rc.update(db.getConfig());
		db.getConfig().save();

		checkFile(new File(db.getDirectory(), "config"), "[core]\n"
				+ "\trepositoryformatversion = 0\n" + "\tfilemode = true\n"
				+ "[remote \"origin\"]\n" + "\turl = /some/dir\n"
				+ "\tfetch = +refs/heads/*:refs/remotes/origin/*\n");
	}

	public void testSaveAddURI() throws Exception {
		writeConfig("[remote \"spearce\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "fetch = +refs/heads/*:refs/remotes/spearce/*\n");

		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "spearce");
		rc.addURI(new URIish("/some/dir"));
		assertEquals(2, rc.getURIs().size());
		rc.update(db.getConfig());
		db.getConfig().save();

		checkFile(new File(db.getDirectory(), "config"), "[core]\n"
				+ "\trepositoryformatversion = 0\n" + "\tfilemode = true\n"
				+ "[remote \"spearce\"]\n"
				+ "\turl = http://www.spearce.org/egit.git\n"
				+ "\turl = /some/dir\n"
				+ "\tfetch = +refs/heads/*:refs/remotes/spearce/*\n");
	}

	public void testSaveRemoveLastURI() throws Exception {
		writeConfig("[remote \"spearce\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "url = /some/dir\n"
				+ "fetch = +refs/heads/*:refs/remotes/spearce/*\n");

		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "spearce");
		assertEquals(2, rc.getURIs().size());
		rc.removeURI(new URIish("/some/dir"));
		assertEquals(1, rc.getURIs().size());
		rc.update(db.getConfig());
		db.getConfig().save();

		checkFile(new File(db.getDirectory(), "config"), "[core]\n"
				+ "\trepositoryformatversion = 0\n" + "\tfilemode = true\n"
				+ "[remote \"spearce\"]\n"
				+ "\turl = http://www.spearce.org/egit.git\n"
				+ "\tfetch = +refs/heads/*:refs/remotes/spearce/*\n");
	}

	public void testSaveRemoveFirstURI() throws Exception {
		writeConfig("[remote \"spearce\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "url = /some/dir\n"
				+ "fetch = +refs/heads/*:refs/remotes/spearce/*\n");

		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "spearce");
		assertEquals(2, rc.getURIs().size());
		rc.removeURI(new URIish("http://www.spearce.org/egit.git"));
		assertEquals(1, rc.getURIs().size());
		rc.update(db.getConfig());
		db.getConfig().save();

		checkFile(new File(db.getDirectory(), "config"), "[core]\n"
				+ "\trepositoryformatversion = 0\n" + "\tfilemode = true\n"
				+ "[remote \"spearce\"]\n" + "\turl = /some/dir\n"
				+ "\tfetch = +refs/heads/*:refs/remotes/spearce/*\n");
	}

	public void testSaveNoTags() throws Exception {
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "origin");
		rc.addURI(new URIish("/some/dir"));
		rc.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/"
				+ rc.getName() + "/*"));
		rc.setTagOpt(TagOpt.NO_TAGS);
		rc.update(db.getConfig());
		db.getConfig().save();

		checkFile(new File(db.getDirectory(), "config"), "[core]\n"
				+ "\trepositoryformatversion = 0\n" + "\tfilemode = true\n"
				+ "[remote \"origin\"]\n" + "\turl = /some/dir\n"
				+ "\tfetch = +refs/heads/*:refs/remotes/origin/*\n"
				+ "\ttagopt = --no-tags\n");
	}

	public void testSaveAllTags() throws Exception {
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "origin");
		rc.addURI(new URIish("/some/dir"));
		rc.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/"
				+ rc.getName() + "/*"));
		rc.setTagOpt(TagOpt.FETCH_TAGS);
		rc.update(db.getConfig());
		db.getConfig().save();

		checkFile(new File(db.getDirectory(), "config"), "[core]\n"
				+ "\trepositoryformatversion = 0\n" + "\tfilemode = true\n"
				+ "[remote \"origin\"]\n" + "\turl = /some/dir\n"
				+ "\tfetch = +refs/heads/*:refs/remotes/origin/*\n"
				+ "\ttagopt = --tags\n");
	}

	public void testSimpleTimeout() throws Exception {
		writeConfig("[remote \"spearce\"]\n"
				+ "url = http://www.spearce.org/egit.git\n"
				+ "fetch = +refs/heads/*:refs/remotes/spearce/*\n"
				+ "timeout = 12\n");
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "spearce");
		assertEquals(12, rc.getTimeout());
	}

	public void testSaveTimeout() throws Exception {
		final RemoteConfig rc = new RemoteConfig(db.getConfig(), "origin");
		rc.addURI(new URIish("/some/dir"));
		rc.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/"
				+ rc.getName() + "/*"));
		rc.setTimeout(60);
		rc.update(db.getConfig());
		db.getConfig().save();

		checkFile(new File(db.getDirectory(), "config"), "[core]\n"
				+ "\trepositoryformatversion = 0\n" + "\tfilemode = true\n"
				+ "[remote \"origin\"]\n" + "\turl = /some/dir\n"
				+ "\tfetch = +refs/heads/*:refs/remotes/origin/*\n"
				+ "\ttimeout = 60\n");
	}
}
