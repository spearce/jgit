/*
 *  Copyright (C) 2007  Robin Rosenberg
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
package org.spearce.jgit.lib;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Test parsing of git remotes
 */
public class RemoteSpecTest extends RepositoryTestCase {

	/**
	 * Test simplest case
	 *
	 * @throws Exception
	 *
	 */
	public void testSimplestOk() throws Exception {
		RemoteSpec spec = new RemoteSpec("their", "git://foo.bar/zip.git",
				"refs/heads/master:refs/heads/origin",null);
		assertEquals("refs/heads/master", spec.getFetchRemoteRef());
		assertEquals("refs/heads/origin", spec.getFetchLocalRef());
		assertFalse(spec.isFetchMatchAny());
		assertFalse(spec.isFetchOverwriteAlways());
	}

	/**
	 * Test a standard case
	 *
	 * @throws Exception
	 */
	public void testStandardOk() throws Exception {
		RemoteSpec spec = new RemoteSpec("their", "git://example.com/zip.git",
				"+refs/heads/master/*:refs/remotes/origin/*",null);
		assertEquals("git://example.com/zip.git", spec.getUrl());
		assertEquals("refs/heads/master", spec.getFetchRemoteRef());
		assertEquals("refs/remotes/origin", spec.getFetchLocalRef());
		assertTrue(spec.isFetchMatchAny());
		assertTrue(spec.isFetchOverwriteAlways());
	}

	/**
	 * Test a <quote>safer</quote> almost standard case
	 *
	 * @throws Exception
	 */
	public void testNonStandardSaferOk() throws Exception {
		RemoteSpec spec = new RemoteSpec("their", "git://example.com/zip.git",
				"refs/heads/master/*:refs/remotes/origin/*",null);
		assertEquals("git://example.com/zip.git", spec.getUrl());
		assertEquals("refs/heads/master", spec.getFetchRemoteRef());
		assertEquals("refs/remotes/origin", spec.getFetchLocalRef());
		assertTrue(spec.isFetchMatchAny());
		assertFalse(spec.isFetchOverwriteAlways());
	}

	/**
	 * Test a case copied from a real Git repo
	 *
	 * @throws Exception
	 */
	public void testReadFromConfig() throws Exception {
		File file = new File(db.getDirectory(),"config");
		FileOutputStream stream = new FileOutputStream(file,true);
		try {
			stream.write(("[remote \"spearce\"]\n"+
		"url = http://www.spearce.org/projects/scm/egit.git\n"+
		"fetch = +refs/heads/*:refs/remotes/spearce/*\n").getBytes());
		} finally {
			stream.close();
		}
		db.getConfig().load();
		RemoteSpec remoteSpec = db.getRemoteSpec("spearce");
		assertEquals("http://www.spearce.org/projects/scm/egit.git", remoteSpec.getUrl());
		assertEquals("refs/heads", remoteSpec.getFetchRemoteRef());
		assertEquals("refs/remotes/spearce", remoteSpec.getFetchLocalRef());
		assertTrue(remoteSpec.isFetchMatchAny());
		assertTrue(remoteSpec.isFetchOverwriteAlways());
	}
}
