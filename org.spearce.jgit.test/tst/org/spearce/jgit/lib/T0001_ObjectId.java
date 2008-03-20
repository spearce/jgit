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

import junit.framework.TestCase;

public class T0001_ObjectId extends TestCase {
	public void test001_toString() {
		final String x = "def4c620bc3713bb1bb26b808ec9312548e73946";
		final ObjectId oid = ObjectId.fromString(x);
		assertEquals(x, oid.toString());
	}

	public void test002_toString() {
		final String x = "ff00eedd003713bb1bb26b808ec9312548e73946";
		final ObjectId oid = ObjectId.fromString(x);
		assertEquals(x, oid.toString());
	}

	public void test003_equals() {
		final String x = "def4c620bc3713bb1bb26b808ec9312548e73946";
		final ObjectId a = ObjectId.fromString(x);
		final ObjectId b = ObjectId.fromString(x);
		assertEquals(a.hashCode(), b.hashCode());
		assertTrue("a and b are same", a.equals(b));
	}

	public void test004_isId() {
		assertTrue("valid id", ObjectId
				.isId("def4c620bc3713bb1bb26b808ec9312548e73946"));
	}

	public void test005_notIsId() {
		assertFalse("bob is not an id", ObjectId.isId("bob"));
	}

	public void test006_notIsId() {
		assertFalse("39 digits is not an id", ObjectId
				.isId("def4c620bc3713bb1bb26b808ec9312548e7394"));
	}

	public void test007_notIsId() {
		assertFalse("uppercase is not accepted", ObjectId
				.isId("Def4c620bc3713bb1bb26b808ec9312548e73946"));
	}

	public void test008_notIsId() {
		assertFalse("g is not a valid hex digit", ObjectId
				.isId("gef4c620bc3713bb1bb26b808ec9312548e73946"));
	}

	public void test009_toString() {
		final String x = "ff00eedd003713bb1bb26b808ec9312548e73946";
		final ObjectId oid = ObjectId.fromString(x);
		assertEquals(x, ObjectId.toString(oid));
	}

	public void test010_toString() {
		final String x = "0000000000000000000000000000000000000000";
		assertEquals(x, ObjectId.toString(null));
	}
}
