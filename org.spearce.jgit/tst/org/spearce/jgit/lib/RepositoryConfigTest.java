package org.spearce.jgit.lib;

import java.io.IOException;

public class RepositoryConfigTest extends RepositoryTestCase {
	public void test001_ReadBareKey() throws IOException {
		String path = writeTrashFile("config_001", "[foo]\nbar\n").getAbsolutePath();
		RepositoryConfig repositoryConfig = new RepositoryConfig(path);
		System.out.println(repositoryConfig.getString("foo", "bar"));
		assertEquals(true, repositoryConfig.getBoolean("foo", "bar", false));
		assertEquals("", repositoryConfig.getString("foo", "bar"));
	}
}
