/*******************************************************************************
 * Copyright (C) 2009, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.spearce.egit.core.op.ConnectProviderOperation;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.core.test.GitTestCase;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.treewalk.TreeWalk;
import org.spearce.jgit.treewalk.WorkingTreeIterator;
import org.spearce.jgit.treewalk.filter.PathFilterGroup;

public class T0003_AdaptableFileTreeIteratorTest extends GitTestCase {

	private Repository repository;

	private File repositoryRoot;

	private File file;

	protected void setUp() throws Exception {
		super.setUp();

		repository = new Repository(gitDir);
		repositoryRoot = repository.getWorkDir();
		repository.create();

		file = new File(project.getProject().getLocation().toFile(), "a.txt");
		final FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("aaaaaaaaaaa");
		fileWriter.close();

		final ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject());
		operation.run(null);
	}

	public void testFileTreeToContainerAdaptation() throws IOException {
		final IWorkspaceRoot root = project.getProject().getWorkspace()
				.getRoot();

		final TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(new AdaptableFileTreeIterator(repositoryRoot, root));
		treeWalk.setRecursive(true);

		final IFile eclipseFile = project.getProject().getFile(file.getName());
		final RepositoryMapping mapping = RepositoryMapping
				.getMapping(eclipseFile);
		final Set<String> repositoryPaths = Collections.singleton(mapping
				.getRepoRelativePath(eclipseFile));

		assertTrue(repositoryPaths.size() == 1);
		treeWalk.setFilter(PathFilterGroup.createFromStrings(repositoryPaths));

		assertTrue(treeWalk.next());

		final WorkingTreeIterator iterator = treeWalk.getTree(1,
				WorkingTreeIterator.class);
		assertTrue(iterator instanceof ContainerTreeIterator);
	}
}
