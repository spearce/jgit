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
package org.spearce.jgit.fetch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.fetch.IndexPack;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.PackFile;
import org.spearce.jgit.lib.RepositoryTestCase;
import org.spearce.jgit.lib.TextProgressMonitor;

/**
 * Test indexing of git packs. A pack is read from a stream, copied
 * to a new pack and an index is created. Then the packs are tested
 * to make sure they contain the expected objects (well we don't test
 * for all of them unless the packs are very small).
 */
public class IndexPackTest extends RepositoryTestCase {

	/**
	 * Test indexing one of the test packs in the egit repo. It has deltas.
	 *
	 * @throws IOException
	 */
	public void test1() throws  IOException {
		File packFile = new File("tst/pack-34be9032ac282b11fa9babdc2b2a93ca996c9c2f.pack");
		final InputStream is = new FileInputStream(packFile);
		try {
			IndexPack pack = new IndexPack(is, new File("tmp_pack1"));
			pack.index(new TextProgressMonitor());
			PackFile file = new PackFile(db, new File("tmp_pack1.pack"));
			assertTrue(file.hasObject(new ObjectId("4b825dc642cb6eb9a060e54bf8d69288fbee4904")));
			assertTrue(file.hasObject(new ObjectId("540a36d136cf413e4b064c2b0e0a4db60f77feab")));
			assertTrue(file.hasObject(new ObjectId("5b6e7c66c276e7610d4a73c70ec1a1f7c1003259")));
			assertTrue(file.hasObject(new ObjectId("6ff87c4664981e4397625791c8ea3bbb5f2279a3")));
			assertTrue(file.hasObject(new ObjectId("82c6b885ff600be425b4ea96dee75dca255b69e7")));
			assertTrue(file.hasObject(new ObjectId("902d5476fa249b7abc9d84c611577a81381f0327")));
			assertTrue(file.hasObject(new ObjectId("aabf2ffaec9b497f0950352b3e582d73035c2035")));
			assertTrue(file.hasObject(new ObjectId("c59759f143fb1fe21c197981df75a7ee00290799")));
		} finally {
			is.close();
		}
	}

	/**
	 * This is just another pack. It so happens that we have two convenient pack to
	 * test with in the repository.
	 *
	 * @throws IOException
	 */
	public void test2() throws  IOException {
		File packFile = new File("tst/pack-df2982f284bbabb6bdb59ee3fcc6eb0983e20371.pack");
		final InputStream is = new FileInputStream(packFile);
		try {
			IndexPack pack = new IndexPack(is, new File("tmp_pack2"));
			pack.index(new TextProgressMonitor());
			PackFile file = new PackFile(db, new File("tmp_pack2.pack"));
			assertTrue(file.hasObject(new ObjectId("02ba32d3649e510002c21651936b7077aa75ffa9")));
			assertTrue(file.hasObject(new ObjectId("0966a434eb1a025db6b71485ab63a3bfbea520b6")));
			assertTrue(file.hasObject(new ObjectId("09efc7e59a839528ac7bda9fa020dc9101278680")));
			assertTrue(file.hasObject(new ObjectId("0a3d7772488b6b106fb62813c4d6d627918d9181")));
			assertTrue(file.hasObject(new ObjectId("1004d0d7ac26fbf63050a234c9b88a46075719d3")));
			assertTrue(file.hasObject(new ObjectId("10da5895682013006950e7da534b705252b03be6")));
			assertTrue(file.hasObject(new ObjectId("1203b03dc816ccbb67773f28b3c19318654b0bc8")));
			assertTrue(file.hasObject(new ObjectId("15fae9e651043de0fd1deef588aa3fbf5a7a41c6")));
			assertTrue(file.hasObject(new ObjectId("16f9ec009e5568c435f473ba3a1df732d49ce8c3")));
			assertTrue(file.hasObject(new ObjectId("1fd7d579fb6ae3fe942dc09c2c783443d04cf21e")));
			assertTrue(file.hasObject(new ObjectId("20a8ade77639491ea0bd667bf95de8abf3a434c8")));
			assertTrue(file.hasObject(new ObjectId("2675188fd86978d5bc4d7211698b2118ae3bf658")));
			// and lots more...
		} finally {
			is.close();
		}
	}
}
