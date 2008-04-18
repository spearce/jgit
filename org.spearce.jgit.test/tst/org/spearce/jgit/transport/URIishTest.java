package org.spearce.jgit.transport;

import org.spearce.jgit.transport.URIish;

import junit.framework.TestCase;

public class URIishTest extends TestCase {

	public void testUnixFile() throws Exception {
		URIish u = new URIish("/home/m y");
		assertNull(u.getScheme());
		assertEquals("/home/m y", u.getPath());
	}

	public void testWindowsFile() throws Exception {
		URIish u = new URIish("D:/m y");
		assertNull(u.getScheme());
		assertEquals("D:/m y", u.getPath());
	}

	public void testFileProtoUnix() throws Exception {
		URIish u = new URIish("file:///home/m y");
		assertEquals("file", u.getScheme());
		assertEquals("/home/m y", u.getPath());
	}

	public void testFileProtoWindows() throws Exception {
		URIish u = new URIish("file:///D:/m y");
		assertEquals("file", u.getScheme());
		assertEquals("D:/m y", u.getPath());
	}

	public void testGitProtoUnix() throws Exception {
		URIish u = new URIish("git://example.com/home/m y");
		assertEquals("git", u.getScheme());
		assertEquals("example.com", u.getHost());
		assertEquals("/home/m y", u.getPath());
	}

	public void testGitProtoUnixPort() throws Exception {
		URIish u = new URIish("git://example.com:333/home/m y");
		assertEquals("git", u.getScheme());
		assertEquals("example.com", u.getHost());
		assertEquals("/home/m y", u.getPath());
		assertEquals(333, u.getPort());
	}

	public void testGitProtoWindowsPort() throws Exception {
		URIish u = new URIish("git://example.com:338/D:/m y");
		assertEquals("git", u.getScheme());
		assertEquals("D:/m y", u.getPath());
		assertEquals(338, u.getPort());
		assertEquals("example.com", u.getHost());
	}

	public void testGitProtoWindows() throws Exception {
		URIish u = new URIish("git://example.com/D:/m y");
		assertEquals("git", u.getScheme());
		assertEquals("D:/m y", u.getPath());
		assertEquals("example.com", u.getHost());
		assertEquals(-1, u.getPort());
	}

	public void testSshProto() throws Exception {
		URIish u = new URIish("ssh://example.com/some/p ath");
		assertEquals("ssh", u.getScheme());
		assertEquals("/some/p ath", u.getPath());
		assertEquals("example.com", u.getHost());
		assertEquals(-1, u.getPort());
	}

	public void testSshProtoWithUserAndPort() throws Exception {
		URIish u = new URIish("ssh://user@example.com:33/some/p ath");
		assertEquals("ssh", u.getScheme());
		assertEquals("/some/p ath", u.getPath());
		assertEquals("example.com", u.getHost());
		assertEquals("user", u.getUser());
		assertNull(u.getPass());
		assertEquals(33, u.getPort());
	}

	public void testSshProtoWithUserPassAndPort() throws Exception {
		URIish u = new URIish("ssh://user:pass@example.com:33/some/p ath");
		assertEquals("ssh", u.getScheme());
		assertEquals("/some/p ath", u.getPath());
		assertEquals("example.com", u.getHost());
		assertEquals("user", u.getUser());
		assertEquals("pass", u.getPass());
		assertEquals(33, u.getPort());
	}
}
