/*
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.pgm;

import java.io.File;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.spearce.jgit.lib.Repository;

@Command(common = false, usage = "Server side backend for 'jgit fetch'")
class UploadPack extends TextBuiltin {
	@Option(name = "--timeout", metaVar = "SECONDS", usage = "abort connection if no activity")
	int timeout = -1;

	@Argument(index = 0, required = true, metaVar = "DIRECTORY", usage = "Repository to read from")
	File srcGitdir;

	@Override
	protected final boolean requiresRepository() {
		return false;
	}

	@Override
	protected void run() throws Exception {
		final org.spearce.jgit.transport.UploadPack rp;

		if (new File(srcGitdir, ".git").isDirectory())
			srcGitdir = new File(srcGitdir, ".git");
		db = new Repository(srcGitdir);
		if (!db.getObjectsDirectory().isDirectory())
			throw die("'" + srcGitdir.getPath() + "' not a git repository");
		rp = new org.spearce.jgit.transport.UploadPack(db);
		if (0 <= timeout)
			rp.setTimeout(timeout);
		rp.upload(System.in, System.out, System.err);
	}
}
