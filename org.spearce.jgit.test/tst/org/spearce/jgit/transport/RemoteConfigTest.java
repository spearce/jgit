/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
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
}
