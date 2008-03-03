package org.spearce.jgit.fetch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryTestCase;

public class FetchTest extends RepositoryTestCase {
	public void testSimpleFullClone() throws IOException, InterruptedException {
		File newTestRepo = new File(trashParent, "new"+System.currentTimeMillis()+"/.git");
		assertFalse(newTestRepo.exists());

		File unusedDir = new File(trashParent, "tmp"+System.currentTimeMillis());
		assertTrue(unusedDir.mkdirs());

		final Repository newRepo = new Repository(newTestRepo);
		newRepo.create();

		Process process = Runtime.getRuntime().exec("git-upload-pack "+trash_git.getAbsolutePath(), null, unusedDir);
		InputStream inputStream = process.getInputStream();
		OutputStream outpuStream = process.getOutputStream();
		final PipedInputStream pi = new PipedInputStream();
		PipedOutputStream po = new PipedOutputStream(pi);
		FetchClient client = new FetchClient(newRepo, null, outpuStream, inputStream, po);
		final Thread fetchThread = Thread.currentThread();
		final Throwable[] threadError = new Throwable[1];
		Thread indexThread = new Thread() {
			@Override
			public void run() {
				IndexPack pack;
				try {
					pack = new IndexPack(pi, new File("tmp_pack1"));
					pack.index();
					pack.renamePack(newRepo);
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					fetchThread.interrupt();
					threadError[0] = e;
				}
			}
		};
		indexThread.start();
		client.run();
		po.close();
		indexThread.join();
		if (threadError[0] != null)
			throw new Error(threadError[0]);
		newRepo.scanForPacks();
		client.updateRemoteRefs("origintest");
		assertEquals("6db9c2ebf75590eef973081736730a9ea169a0c4", newRepo.mapCommit("remotes/origintest/a").getCommitId().toString());
		assertEquals("17768080a2318cd89bba4c8b87834401e2095703", newRepo.mapTag("refs/tags/B").getTagId().toString());
		assertEquals("d86a2aada2f5e7ccf6f11880bfb9ab404e8a8864", newRepo.mapTag("refs/tags/B").getObjId().toString());
		assertEquals("6db9c2ebf75590eef973081736730a9ea169a0c4", newRepo.mapTag("refs/tags/A").getObjId().toString());
		assertNull(newRepo.mapTag("refs/tags/A").getTagId());
	}
}
