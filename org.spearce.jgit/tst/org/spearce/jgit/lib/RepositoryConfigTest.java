package org.spearce.jgit.lib;

import java.io.IOException;

/**
 * Test reading of git config
 */
public class RepositoryConfigTest extends RepositoryTestCase {
	/**
	 * Read config item with no value from a section without a subsection.
	 *
	 * @throws IOException
	 */
	public void test001_ReadBareKey() throws IOException {
		String path = writeTrashFile("config_001", "[foo]\nbar\n").getAbsolutePath();
		RepositoryConfig repositoryConfig = new RepositoryConfig(path);
		System.out.println(repositoryConfig.getString("foo", null, "bar"));
		assertEquals(true, repositoryConfig.getBoolean("foo", null, "bar", false));
		assertEquals("", repositoryConfig.getString("foo", null, "bar"));
	}

	/**
	 * Read various data from a subsection.
	 *
	 * @throws IOException
	 */
	public void test002_ReadWithSubsection() throws IOException {
		String path = writeTrashFile("config_002", "[foo \"zip\"]\nbar\n[foo \"zap\"]\nbar=false\nn=3\n").getAbsolutePath();
		RepositoryConfig repositoryConfig = new RepositoryConfig(path);
		assertEquals(true, repositoryConfig.getBoolean("foo", "zip", "bar", false));
		assertEquals("", repositoryConfig.getString("foo","zip", "bar"));
		assertEquals(false, repositoryConfig.getBoolean("foo", "zap", "bar", true));
		assertEquals("false", repositoryConfig.getString("foo", "zap", "bar"));
		assertEquals(3, repositoryConfig.getInt("foo", "zap", "n", 4));
		assertEquals(4, repositoryConfig.getInt("foo", "zap","m", 4));
	}
}
