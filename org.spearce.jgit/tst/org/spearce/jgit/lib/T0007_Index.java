package org.spearce.jgit.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.lib.GitIndex.Entry;

public class T0007_Index extends RepositoryTestCase {

	private int system(File dir, String cmd) throws IOException,
			InterruptedException {
		final Process process = Runtime.getRuntime().exec(cmd, null, dir);
		new Thread() {
			public void run() {
				try {
					InputStream s = process.getErrorStream();
					for (int c = s.read(); c != -1; c = s.read()) {
						System.err.print((char) c);
					}
					s.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}.start();
		final Thread t2 = new Thread() {
			public void run() {
				synchronized (this) {
					try {
						InputStream e = process.getInputStream();
						for (int c = e.read(); c != -1; c = e.read()) {
							System.out.print((char) c);
						}
						e.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		};
		t2.start();
		process.getOutputStream().close();
		int ret = process.waitFor();
		synchronized (t2) {
			return ret;
		}
	}

	public void testCreateEmptyIndex() throws Exception {
		Repository db = new Repository(trash_git);
		GitIndex index = new GitIndex(db);
		index.write();
// native git doesn't like an empty index
// assertEquals(0,system(trash,"git status"));
	}

	public void testCreateSimpleSortTestIndex() throws Exception {
		Repository db = new Repository(trash_git);
		GitIndex index = new GitIndex(db);
		writeTrashFile("a/b", "data:a/b");
		writeTrashFile("a:b", "data:a:b");
		writeTrashFile("a.b", "data:a.b");
		index.add(trash, new File(trash, "a/b"));
		index.add(trash, new File(trash, "a:b"));
		index.add(trash, new File(trash, "a.b"));
		index.write();
		assertEquals(0, system(trash, "git status"));

		assertEquals("a/b", index.getEntry("a/b").getName());
		assertEquals("a:b", index.getEntry("a:b").getName());
		assertEquals("a.b", index.getEntry("a.b").getName());
		assertNull(index.getEntry("a*b"));
	}

	public void testUpdateSimpleSortTestIndex() throws Exception {
		Repository db = new Repository(trash_git);
		GitIndex index = new GitIndex(db);
		writeTrashFile("a/b", "data:a/b");
		writeTrashFile("a:b", "data:a:b");
		writeTrashFile("a.b", "data:a.b");
		index.add(trash, new File(trash, "a/b"));
		index.add(trash, new File(trash, "a:b"));
		index.add(trash, new File(trash, "a.b"));
		writeTrashFile("a/b", "data:a/b modified");
		index.add(trash, new File(trash, "a/b"));
		index.write();
		assertEquals(0, system(trash, "git status"));
	}

	public void testWriteTree() throws Exception {
		Repository db = new Repository(trash_git);
		GitIndex index = new GitIndex(db);
		writeTrashFile("a/b", "data:a/b");
		writeTrashFile("a:b", "data:a:b");
		writeTrashFile("a.b", "data:a.b");
		index.add(trash, new File(trash, "a/b"));
		index.add(trash, new File(trash, "a:b"));
		index.add(trash, new File(trash, "a.b"));
		index.write();
		assertEquals(0, system(trash, "git status"));
		ObjectId id = index.writeTree(trash);
		assertEquals("c696abc3ab8e091c665f49d00eb8919690b3aec3", id.toString());
		
		writeTrashFile("a/b", "data:a/b");
		index.add(trash, new File(trash, "a/b"));
	}

	public void testReadTree() throws Exception {
		// Prepare tree
		Repository db = new Repository(trash_git);
		GitIndex index = new GitIndex(db);
		writeTrashFile("a/b", "data:a/b");
		writeTrashFile("a:b", "data:a:b");
		writeTrashFile("a.b", "data:a.b");
		index.add(trash, new File(trash, "a/b"));
		index.add(trash, new File(trash, "a:b"));
		index.add(trash, new File(trash, "a.b"));
		index.write();
		assertEquals(0, system(trash, "git status"));
		ObjectId id = index.writeTree(trash);
		System.out.println("wrote id " + id);
		assertEquals("c696abc3ab8e091c665f49d00eb8919690b3aec3", id.toString());
		GitIndex index2 = new GitIndex(db);

		index2.readTree(db.mapTree(new ObjectId(
				"c696abc3ab8e091c665f49d00eb8919690b3aec3")));
		Entry[] members = index2.getMembers();
		assertEquals(3, members.length);
		assertEquals("a.b", members[0].getName());
		assertEquals("a/b", members[1].getName());
		assertEquals("a:b", members[2].getName());
		assertEquals(3, members.length);

	}

	public void testReadTree2() throws Exception {
		// Prepare a larger tree to test some odd cases in tree writing
		Repository db = new Repository(trash_git);
		GitIndex index = new GitIndex(db);
		File f1 = writeTrashFile("a/a/a/a", "data:a/a/a/a");
		File f2 = writeTrashFile("a/c/c", "data:a/c/c");
		File f3 = writeTrashFile("a/b", "data:a/b");
		File f4 = writeTrashFile("a:b", "data:a:b");
		File f5 = writeTrashFile("a/d", "data:a/d");
		File f6 = writeTrashFile("a.b", "data:a.b");
		index.add(trash, f1);
		index.add(trash, f2);
		index.add(trash, f3);
		index.add(trash, f4);
		index.add(trash, f5);
		index.add(trash, f6);
		index.write();
		ObjectId id = index.writeTree(trash);
		System.out.println("wrote id " + id);
		assertEquals("ba78e065e2c261d4f7b8f42107588051e87e18e9", id.toString());
		GitIndex index2 = new GitIndex(db);

		index2.readTree(db.mapTree(new ObjectId(
				"ba78e065e2c261d4f7b8f42107588051e87e18e9")));
		Entry[] members = index2.getMembers();
		assertEquals(6, members.length);
		assertEquals("a.b", members[0].getName());
		assertEquals("a/a/a/a", members[1].getName());
		assertEquals("a/b", members[2].getName());
		assertEquals("a/c/c", members[3].getName());
		assertEquals("a/d", members[4].getName());
		assertEquals("a:b", members[5].getName());
	}

	public void testDelete() throws Exception {
		Repository db = new Repository(trash_git);
		GitIndex index = new GitIndex(db);
		writeTrashFile("a/b", "data:a/b");
		writeTrashFile("a:b", "data:a:b");
		writeTrashFile("a.b", "data:a.b");
		index.add(trash, new File(trash, "a/b"));
		index.add(trash, new File(trash, "a:b"));
		index.add(trash, new File(trash, "a.b"));
		index.write();
		assertEquals(0, system(trash, "git status"));
		index.writeTree(trash);
		index.remove(trash, new File(trash, "a:b"));
		index.write();
		assertEquals("a.b", index.getMembers()[0].getName());
		assertEquals("a/b", index.getMembers()[1].getName());
	}

	public void testCheckout() throws Exception {
		// Prepare tree, remote it and checkout
		Repository db = new Repository(trash_git);
		GitIndex index = new GitIndex(db);
		File aslashb = writeTrashFile("a/b", "data:a/b");
		File acolonb = writeTrashFile("a:b", "data:a:b");
		File adotb = writeTrashFile("a.b", "data:a.b");
		index.add(trash, aslashb);
		index.add(trash, acolonb);
		index.add(trash, adotb);
		index.write();
		assertEquals(0, system(trash, "git status"));
		index.writeTree(trash);
		delete(aslashb);
		delete(acolonb);
		delete(adotb);
		delete(aslashb.getParentFile());

		GitIndex index2 = new GitIndex(db);
		assertEquals(0, index2.getMembers().length);

		index2.readTree(db.mapTree(new ObjectId(
				"c696abc3ab8e091c665f49d00eb8919690b3aec3")));

		index2.checkout(trash);
		assertEquals("data:a/b", content(aslashb));
		assertEquals("data:a:b", content(acolonb));
		assertEquals("data:a.b", content(adotb));
	}

	private String content(File f) throws IOException {
		byte[] buf = new byte[(int) f.length()];
		FileInputStream is = new FileInputStream(f);
		int read = is.read(buf);
		assertEquals(f.length(), read);
		return new String(buf, 0);
	}

	private void delete(File f) throws IOException {
		if (!f.delete())
			throw new IOException("Failed to delete f");
	}
}
