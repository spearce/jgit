/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.internal.mapping;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.op.ConnectProviderOperation;
import org.spearce.egit.core.test.GitTestCase;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;

public class T0002_history extends GitTestCase {

	protected static final PersonIdent jauthor;

	protected static final PersonIdent jcommitter;

	static {
		jauthor = new PersonIdent("J. Author", "jauthor@example.com");
		jcommitter = new PersonIdent("J. Committer", "jcommitter@example.com");
	}

	private File workDir;
	private File gitDir;
	private Repository thisGit;
	private Tree tree;
	private ObjectWriter objectWriter;

	protected void setUp() throws Exception {
		super.setUp();
		project.createSourceFolder();
		gitDir = new File(project.getProject().getWorkspace().getRoot()
				.getRawLocation().toFile(), ".git");
		thisGit = new Repository(gitDir);
		workDir = thisGit.getWorkDir();
		thisGit.create();
		objectWriter = new ObjectWriter(thisGit);

		tree = new Tree(thisGit);
		Tree projectTree = tree.addTree("Project-1");
		File project1_a_txt = createFile("Project-1/A.txt","A.txt - first version\n");
		addFile(projectTree,project1_a_txt);
		projectTree.setId(objectWriter.writeTree(projectTree));
		tree.setId(objectWriter.writeTree(tree));
		Commit commit = new Commit(thisGit);
		commit.setAuthor(new PersonIdent(jauthor, new Date(0L), TimeZone
				.getTimeZone("GMT+1")));
		commit.setCommitter(new PersonIdent(jcommitter, new Date(0L), TimeZone
				.getTimeZone("GMT+1")));
		commit.setMessage("Foo\n\nMessage");
		commit.setTree(tree);
		ObjectId commitId = objectWriter.writeCommit(commit);
		RefUpdate lck = thisGit.updateRef("refs/heads/master");
		assertNotNull("obtained lock", lck);
		lck.setNewObjectId(commitId);
		assertEquals(RefUpdate.Result.NEW, lck.forceUpdate());

		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject());
		operation.run(null);
	}

	private void addFile(Tree t,File f) throws IOException {
		ObjectId id = objectWriter.writeBlob(f);
		t.addEntry(new FileTreeEntry(t,id,f.getName().getBytes("UTF-8"),false));
	}

	private File createFile(String name, String content) throws IOException {
		File f = new File(workDir, name);
		FileWriter fileWriter = new FileWriter(f);
		fileWriter.write(content);
		fileWriter.close();
		return f;
	}

	public void testShallowHistory() {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/A.txt"), IFileHistoryProvider.SINGLE_LINE_OF_DESCENT, new NullProgressMonitor());
		IFileRevision[] fileRevisions = fileHistory.getFileRevisions();
		assertEquals(1, fileRevisions.length);
		assertEquals("6dd8f0b51204fa24a01734971947847549ec4ba8", fileRevisions[0].getContentIdentifier());
	}
}
