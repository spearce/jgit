/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.core.test;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public abstract class GitTestCase extends TestCase {

	protected TestProject project;

	protected File gitDir;

	protected void setUp() throws Exception {
		super.setUp();
		project = new TestProject();
		gitDir = new File(project.getProject().getWorkspace().getRoot()
				.getRawLocation().toFile(), ".git");
		rmrf(gitDir);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		project.dispose();
		rmrf(gitDir);
	}

	private void rmrf(File d) throws IOException {
		if (!d.exists())
			return;

		File[] files = d.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; ++i) {
				if (files[i].isDirectory())
					rmrf(files[i]);
				else if (!files[i].delete())
					throw new IOException(files[i] + " in use or undeletable");
			}
		}
		if (!d.delete())
			throw new IOException(d + " in use or undeletable");
		assert !d.exists();
	}

}
