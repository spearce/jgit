/*
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;

import junit.framework.TestCase;
import org.spearce.jgit.util.JGitTestUtil;

public abstract class RepositoryTestCase extends TestCase {

	protected final File trashParent = new File("trash");

	protected File trash;

	protected File trash_git;

	protected static final PersonIdent jauthor;

	protected static final PersonIdent jcommitter;

	static {
		jauthor = new PersonIdent("J. Author", "jauthor@example.com");
		jcommitter = new PersonIdent("J. Committer", "jcommitter@example.com");
	}

	protected static void recursiveDelete(final File dir) {
		final File[] ls = dir.listFiles();
		if (ls != null) {
			for (int k = 0; k < ls.length; k++) {
				final File e = ls[k];
				if (e.isDirectory()) {
					recursiveDelete(e);
				} else {
					e.delete();
				}
			}
		}
		dir.delete();
		if (dir.exists()) {
			System.out.println("Warning: Failed to delete " + dir);
		}
	}

	protected static void copyFile(final File src, final File dst)
			throws IOException {
		final FileInputStream fis = new FileInputStream(src);
		final FileOutputStream fos = new FileOutputStream(dst);
		final byte[] buf = new byte[4096];
		int r;
		while ((r = fis.read(buf)) > 0) {
			fos.write(buf, 0, r);
		}
		fis.close();
		fos.close();
	}

	protected File writeTrashFile(final String name, final String data)
			throws IOException {
		File tf = new File(trash, name);
		File tfp = tf.getParentFile();
		if (!tfp.exists() && !tf.getParentFile().mkdirs())
			throw new Error("Could not create directory " + tf.getParentFile());
		final OutputStreamWriter fw = new OutputStreamWriter(
				new FileOutputStream(tf), "UTF-8");
		fw.write(data);
		fw.close();
		return tf;
	}

	protected static void checkFile(File f, final String checkData)
			throws IOException {
		Reader r = new InputStreamReader(new FileInputStream(f), "ISO-8859-1");
		char[] data = new char[(int) f.length()];
		if (f.length() !=  r.read(data))
			throw new IOException("Internal error reading file data from "+f);
		assertEquals(checkData, new String(data));
	}

	protected Repository db;

	public void setUp() throws Exception {
		super.setUp();
		recursiveDelete(trashParent);
		trash = new File(trashParent,"trash"+System.currentTimeMillis());
		trash_git = new File(trash, ".git");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				recursiveDelete(trashParent);
			}
		});

		db = new Repository(trash_git);
		db.create();

		final String[] packs = {
				"pack-34be9032ac282b11fa9babdc2b2a93ca996c9c2f",
				"pack-df2982f284bbabb6bdb59ee3fcc6eb0983e20371",
				"pack-9fb5b411fe6dfa89cc2e6b89d2bd8e5de02b5745",
				"pack-e6d07037cbcf13376308a0a995d1fa48f8f76aaa"
		};
		final File packDir = new File(db.getObjectsDirectory(), "pack");
		for (int k = 0; k < packs.length; k++) {
			copyFile(JGitTestUtil.getTestResourceFile(packs[k] + ".pack"), new File(packDir,
					packs[k] + ".pack"));
			copyFile(JGitTestUtil.getTestResourceFile(packs[k] + ".idx"), new File(packDir,
					packs[k] + ".idx"));
		}

		copyFile(JGitTestUtil.getTestResourceFile("packed-refs"), new File(trash_git,"packed-refs"));

		db.scanForPacks();
	}

	protected void tearDown() throws Exception {
		db.close();
		super.tearDown();
	}

	/**
	 * Helper for creating extra empty repos
	 *
	 * @return a new empty git repository for testing purposes
	 *
	 * @throws IOException
	 */
	protected Repository createNewEmptyRepo() throws IOException {
		File newTestRepo = new File(trashParent, "new"+System.currentTimeMillis()+"/.git");
		assertFalse(newTestRepo.exists());
		File unusedDir = new File(trashParent, "tmp"+System.currentTimeMillis());
		assertTrue(unusedDir.mkdirs());
		final Repository newRepo = new Repository(newTestRepo);
		newRepo.create();
		return newRepo;
	}

}
