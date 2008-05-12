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

import junit.framework.TestCase;

import org.spearce.jgit.lib.Ref;

public class RefSpecTestCase extends TestCase {
	public void testMasterMaster() {
		final String sn = "refs/heads/master";
		final RefSpec rs = new RefSpec(sn + ":" + sn);
		assertFalse(rs.isForceUpdate());
		assertFalse(rs.isWildcard());
		assertEquals(sn, rs.getSource());
		assertEquals(sn, rs.getDestination());
		assertEquals(sn + ":" + sn, rs.toString());
		assertEquals(rs, new RefSpec(rs.toString()));

		Ref r = new Ref(sn, null);
		assertTrue(rs.matchSource(r));
		assertTrue(rs.matchDestination(r));
		assertSame(rs, rs.expandFromSource(r));

		r = new Ref(sn + "-and-more", null);
		assertFalse(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
	}

	public void testForceMasterMaster() {
		final String sn = "refs/heads/master";
		final RefSpec rs = new RefSpec("+" + sn + ":" + sn);
		assertTrue(rs.isForceUpdate());
		assertFalse(rs.isWildcard());
		assertEquals(sn, rs.getSource());
		assertEquals(sn, rs.getDestination());
		assertEquals("+" + sn + ":" + sn, rs.toString());
		assertEquals(rs, new RefSpec(rs.toString()));

		Ref r = new Ref(sn, null);
		assertTrue(rs.matchSource(r));
		assertTrue(rs.matchDestination(r));
		assertSame(rs, rs.expandFromSource(r));

		r = new Ref(sn + "-and-more", null);
		assertFalse(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
	}

	public void testMaster() {
		final String sn = "refs/heads/master";
		final RefSpec rs = new RefSpec(sn);
		assertFalse(rs.isForceUpdate());
		assertFalse(rs.isWildcard());
		assertEquals(sn, rs.getSource());
		assertNull(rs.getDestination());
		assertEquals(sn, rs.toString());
		assertEquals(rs, new RefSpec(rs.toString()));

		Ref r = new Ref(sn, null);
		assertTrue(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
		assertSame(rs, rs.expandFromSource(r));

		r = new Ref(sn + "-and-more", null);
		assertFalse(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
	}

	public void testForceMaster() {
		final String sn = "refs/heads/master";
		final RefSpec rs = new RefSpec("+" + sn);
		assertTrue(rs.isForceUpdate());
		assertFalse(rs.isWildcard());
		assertEquals(sn, rs.getSource());
		assertNull(rs.getDestination());
		assertEquals("+" + sn, rs.toString());
		assertEquals(rs, new RefSpec(rs.toString()));

		Ref r = new Ref(sn, null);
		assertTrue(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
		assertSame(rs, rs.expandFromSource(r));

		r = new Ref(sn + "-and-more", null);
		assertFalse(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
	}

	public void testDeleteMaster() {
		final String sn = "refs/heads/master";
		final RefSpec rs = new RefSpec(":" + sn);
		assertFalse(rs.isForceUpdate());
		assertFalse(rs.isWildcard());
		assertNull(rs.getSource());
		assertEquals(sn, rs.getDestination());
		assertEquals(":" + sn, rs.toString());
		assertEquals(rs, new RefSpec(rs.toString()));

		Ref r = new Ref(sn, null);
		assertFalse(rs.matchSource(r));
		assertTrue(rs.matchDestination(r));
		assertSame(rs, rs.expandFromSource(r));

		r = new Ref(sn + "-and-more", null);
		assertFalse(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
	}

	public void testForceRemotesOrigin() {
		final String srcn = "refs/heads/*";
		final String dstn = "refs/remotes/origin/*";
		final RefSpec rs = new RefSpec("+" + srcn + ":" + dstn);
		assertTrue(rs.isForceUpdate());
		assertTrue(rs.isWildcard());
		assertEquals(srcn, rs.getSource());
		assertEquals(dstn, rs.getDestination());
		assertEquals("+" + srcn + ":" + dstn, rs.toString());
		assertEquals(rs, new RefSpec(rs.toString()));

		Ref r;
		RefSpec expanded;

		r = new Ref("refs/heads/master", null);
		assertTrue(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
		expanded = rs.expandFromSource(r);
		assertNotSame(rs, expanded);
		assertTrue(expanded.isForceUpdate());
		assertFalse(expanded.isWildcard());
		assertEquals(r.getName(), expanded.getSource());
		assertEquals("refs/remotes/origin/master", expanded.getDestination());

		r = new Ref("refs/remotes/origin/next", null);
		assertFalse(rs.matchSource(r));
		assertTrue(rs.matchDestination(r));

		r = new Ref("refs/tags/v1.0", null);
		assertFalse(rs.matchSource(r));
		assertFalse(rs.matchDestination(r));
	}

	public void testCreateEmpty() {
		final RefSpec rs = new RefSpec();
		assertFalse(rs.isForceUpdate());
		assertFalse(rs.isWildcard());
		assertEquals("HEAD", rs.getSource());
		assertNull(rs.getDestination());
		assertEquals("HEAD", rs.toString());
	}

	public void testSetForceUpdate() {
		final String s = "refs/heads/*:refs/remotes/origin/*";
		final RefSpec a = new RefSpec(s);
		assertFalse(a.isForceUpdate());
		RefSpec b = a.setForceUpdate(true);
		assertNotSame(a, b);
		assertFalse(a.isForceUpdate());
		assertTrue(b.isForceUpdate());
		assertEquals(s, a.toString());
		assertEquals("+" + s, b.toString());
	}

	public void testSetSource() {
		final RefSpec a = new RefSpec();
		final RefSpec b = a.setSource("refs/heads/master");
		assertNotSame(a, b);
		assertEquals("HEAD", a.toString());
		assertEquals("refs/heads/master", b.toString());
	}

	public void testSetDestination() {
		final RefSpec a = new RefSpec();
		final RefSpec b = a.setDestination("refs/heads/master");
		assertNotSame(a, b);
		assertEquals("HEAD", a.toString());
		assertEquals("HEAD:refs/heads/master", b.toString());
	}

	public void testSetDestination_SourceNull() {
		final RefSpec a = new RefSpec();
		RefSpec b;

		b = a.setDestination("refs/heads/master");
		b = b.setSource(null);
		assertNotSame(a, b);
		assertEquals("HEAD", a.toString());
		assertEquals(":refs/heads/master", b.toString());
	}

	public void testSetSourceDestination() {
		final RefSpec a = new RefSpec();
		final RefSpec b;
		b = a.setSourceDestination("refs/heads/*", "refs/remotes/origin/*");
		assertNotSame(a, b);
		assertEquals("HEAD", a.toString());
		assertEquals("refs/heads/*:refs/remotes/origin/*", b.toString());
	}
}
