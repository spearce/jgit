/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
import java.io.IOException;

public class T0004_PackReader extends RepositoryTestCase {
	private static final String PACK_NAME = "pack-34be9032ac282b11fa9babdc2b2a93ca996c9c2f";
	private static final File TEST_PACK = new File(new File("tst"), PACK_NAME + ".pack");
	private static final File TEST_IDX = new File(TEST_PACK.getParentFile(), PACK_NAME + ".idx");

	public void test003_lookupCompressedObject() throws IOException {
		final PackFile pr;
		final ObjectId id;
		final PackedObjectLoader or;

		id = new ObjectId("902d5476fa249b7abc9d84c611577a81381f0327");
		pr = new PackFile(db, TEST_IDX, TEST_PACK);
		or = pr.get(id);
		assertNotNull(or);
		assertEquals(id, or.getId());
		assertEquals(Constants.OBJ_TREE, or.getType());
		assertEquals(35, or.getSize());
		assertEquals(7738, or.getDataOffset());
		pr.close();
	}

	public void test004_lookupDeltifiedObject() throws IOException {
		final ObjectId id;
		final ObjectLoader or;

		id = new ObjectId("5b6e7c66c276e7610d4a73c70ec1a1f7c1003259");
		or = db.openObject(id);
		assertNotNull(or);
		assertTrue(or instanceof PackedObjectLoader);
		assertEquals(id, or.getId());
		assertEquals(Constants.OBJ_BLOB, or.getType());
		assertEquals(18009, or.getSize());
		assertEquals(537, ((PackedObjectLoader) or).getDataOffset());
	}

	public void test005_todopack() throws IOException {
		final File todopack = new File(new File("tst"), "todopack");
		if (!todopack.isDirectory()) {
			System.err.println("Skipping " + getName() + ": no " + todopack);
			return;
		}

		final File packDir = new File(db.getObjectsDirectory(), "pack");
		final String packname = "pack-2e71952edc41f3ce7921c5e5dd1b64f48204cf35";
		copyFile(new File(todopack, packname + ".pack"), new File(packDir,
				packname + ".pack"));
		copyFile(new File(todopack, packname + ".idx"), new File(packDir,
				packname + ".idx"));
		db.scanForPacks();
		Tree t;

		t = db
				.mapTree(new ObjectId(
						"aac9df07f653dd18b935298deb813e02c32d2e6f"));
		assertNotNull(t);
		t.memberCount();

		t = db
				.mapTree(new ObjectId(
						"6b9ffbebe7b83ac6a61c9477ab941d999f5d0c96"));
		assertNotNull(t);
		t.memberCount();
	}
}
