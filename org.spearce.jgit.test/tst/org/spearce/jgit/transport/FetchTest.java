package org.spearce.jgit.transport;

import java.io.IOException;

import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.LockFile;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryTestCase;
import org.spearce.jgit.lib.TextProgressMonitor;
import org.spearce.jgit.transport.FetchClient;
import org.spearce.jgit.transport.LocalGitProtocolFetchClient;

public class FetchTest extends RepositoryTestCase {

	public void testSimpleFullLocalClone() throws IOException {
		final Repository newRepo = createNewEmptyRepo();
		FetchClient client = LocalGitProtocolFetchClient.create(newRepo, "origintest", trash_git);
		client.setFetchAllTags(true);
		client.run(new TextProgressMonitor());

		assertEquals("6db9c2ebf75590eef973081736730a9ea169a0c4", newRepo.mapCommit("remotes/origintest/a").getCommitId().toString());
		assertEquals("17768080a2318cd89bba4c8b87834401e2095703", newRepo.mapTag("refs/tags/B").getTagId().toString());
		assertEquals("d86a2aada2f5e7ccf6f11880bfb9ab404e8a8864", newRepo.mapTag("refs/tags/B").getObjId().toString());
		assertEquals("6db9c2ebf75590eef973081736730a9ea169a0c4", newRepo.mapTag("refs/tags/A").getObjId().toString());
		assertNull(newRepo.mapTag("refs/tags/A").getTagId());

		// Now do an incremental update test too, a non-fast-forward
		Commit commit = new Commit(db);
		commit.setAuthor(new PersonIdent(jauthor, 100, 1));
		commit.setCommitter(new PersonIdent(jcommitter, 101,2));
		commit.setMessage("Update");
		commit.setTree(db.mapTree("refs/heads/a"));
		ObjectWriter ow = new ObjectWriter(db);
		commit.setCommitId(ow.writeCommit(commit));
		LockFile lockRef = db.lockRef("refs/heads/a");
		lockRef.write(commit.getCommitId());
		if (!lockRef.commit())
			throw new IllegalStateException("Could not update refs/heafs/a in test");

		FetchClient client2 = LocalGitProtocolFetchClient.create(newRepo, "origintest", trash_git);
		client2.run(new TextProgressMonitor());

		assertEquals("9bac73222088373f5a41ed64994adc881a0a27b6", newRepo.mapCommit("remotes/origintest/a").getCommitId().toString());
		// the rest is unchanged
		assertEquals("17768080a2318cd89bba4c8b87834401e2095703", newRepo.mapTag("refs/tags/B").getTagId().toString());
		assertEquals("d86a2aada2f5e7ccf6f11880bfb9ab404e8a8864", newRepo.mapTag("refs/tags/B").getObjId().toString());
		assertEquals("6db9c2ebf75590eef973081736730a9ea169a0c4", newRepo.mapTag("refs/tags/A").getObjId().toString());
		assertNull(newRepo.mapTag("refs/tags/A").getTagId());

		// This is a full clone so we expect all tags, including this one
		assertEquals("fd608fbe625a2b456d9f15c2b1dc41f252057dd7", newRepo.mapTag("refs/tags/spearce-gpg-pub").getObjId().toString());
	}

	public void testSimpleOneBranchClone() throws IOException {
		final Repository newRepo = createNewEmptyRepo();
		FetchClient client = LocalGitProtocolFetchClient.create(newRepo, "origintest", trash_git);
		client.setBranches(new String[] { "refs/heads/pa" });
		client.run(new TextProgressMonitor());

		assertEquals("d86a2aada2f5e7ccf6f11880bfb9ab404e8a8864", newRepo.mapCommit("remotes/origintest/pa").getCommitId().toString());
		assertNull(newRepo.mapCommit("6db9c2ebf75590eef973081736730a9ea169a0c4")); // refs/heads/a
		assertNotNull(newRepo.mapTag("17768080a2318cd89bba4c8b87834401e2095703")); // refs/heads/B
		assertEquals("d86a2aada2f5e7ccf6f11880bfb9ab404e8a8864", newRepo.mapTag("refs/tags/B").getObjId().toString());
		assertNull(newRepo.mapTag("refs/tags/A"));

		// Now do an incremental update test too, a non-fast-forward
		Commit commit = new Commit(db);
		commit.setAuthor(new PersonIdent(jauthor, 100, 1));
		commit.setCommitter(new PersonIdent(jcommitter, 101,2));
		commit.setMessage("Update");
		commit.setTree(db.mapTree("refs/heads/a"));
		ObjectWriter ow = new ObjectWriter(db);
		commit.setCommitId(ow.writeCommit(commit));
		LockFile lockRef = db.lockRef("refs/heads/ap");
		lockRef.write(commit.getCommitId());
		if (!lockRef.commit())
			throw new IllegalStateException("Could not update refs/heads/a in test");

		FetchClient client2 = LocalGitProtocolFetchClient.create(newRepo, "origintest", trash_git);
		client2.setBranches(new String[] { "refs/heads/b", "refs/heads/ap" });
		client2.run(new TextProgressMonitor());

		// The generated commit in this test
		assertEquals("9bac73222088373f5a41ed64994adc881a0a27b6", newRepo.mapCommit("remotes/origintest/ap").getCommitId().toString());
		// the rest is unchanged
		assertEquals("17768080a2318cd89bba4c8b87834401e2095703", newRepo.mapTag("refs/tags/B").getTagId().toString());
		assertEquals("d86a2aada2f5e7ccf6f11880bfb9ab404e8a8864", newRepo.mapTag("refs/tags/B").getObjId().toString());

		// Got some new stuff
		assertEquals("6db9c2ebf75590eef973081736730a9ea169a0c4", newRepo.mapTag("refs/tags/A").getObjId().toString());
		assertNull(newRepo.mapTag("refs/tags/A").getTagId());

		// This tag is not referenced by any commit
		assertNull(newRepo.mapTag("refs/tags/spearc-gpg-pub"));
	}
}
