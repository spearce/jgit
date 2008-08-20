/*
 * Copyright (C) 2007, Charles O'Farrell <charleso@charleso.org>
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.RefComparator;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.RefUpdate.Result;

@Command(common = true, usage = "List, create, or delete branches")
class Branch extends TextBuiltin {

	@Option(name = "--remote", aliases = { "-r" }, usage = "act on remote-tracking branches")
	private boolean remote = false;

	@Option(name = "--all", aliases = { "-a" }, usage = "list both remote-tracking and local branches")
	private boolean all = false;

	@Option(name = "--delete", aliases = { "-d" }, usage = "delete fully merged branch")
	private boolean delete = false;

	@Option(name = "--delete-force", aliases = { "-D" }, usage = "delete branch (even if not merged)")
	private boolean deleteForce = false;

	@Option(name = "--verbose", aliases = { "-v" }, usage = "be verbose")
	private boolean verbose = false;

	@Argument
	private List<String> branches = new ArrayList<String>();

	private final Map<String, Ref> printRefs = new LinkedHashMap<String, Ref>();

	@Override
	protected void run() throws Exception {
		if (delete || deleteForce)
			delete(deleteForce);
		else
			list();
	}

	private void list() {
		Map<String, Ref> refs = db.getAllRefs();
		Ref head = refs.get(Constants.HEAD);
		// This can happen if HEAD is stillborn
		if (head != null) {
			String current = head.getName();
			if (current.equals(Constants.HEAD))
				addRef("(no branch)", head);
			addRefs(refs, Constants.HEADS_PREFIX + '/', !remote);
			addRefs(refs, Constants.REMOTES_PREFIX + '/', remote);
			for (final Entry<String, Ref> e : printRefs.entrySet()) {
				printHead(e.getKey(), current.equals(e.getValue().getName()));
			}
		}
	}

	private void addRefs(final Map<String, Ref> allRefs, final String prefix,
			final boolean add) {
		if (all || add) {
			for (final Ref ref : RefComparator.sort(allRefs.values())) {
				final String name = ref.getName();
				if (name.startsWith(prefix))
					addRef(name, ref);
			}
		}
	}

	private void addRef(final String name, final Ref ref) {
		printRefs.put(name, ref);
	}

	private void printHead(String ref, boolean isCurrent) {
		out.print(isCurrent ? '*' : ' ');
		out.print(' ');
		ref = ref.substring(ref.indexOf('/', 5) + 1);
		out.println(ref);
	}

	private void delete(boolean force) throws IOException {
		String current = db.getBranch();
		ObjectId head = db.resolve(Constants.HEAD);
		for (String branch : branches) {
			if (current.equals(branch)) {
				String err = "Cannot delete the branch '%s' which you are currently on.";
				throw die(String.format(err, branch));
			}
			RefUpdate update = db.updateRef((remote ? Constants.REMOTES_PREFIX
					: Constants.HEADS_PREFIX)
					+ '/' + branch);
			update.setNewObjectId(head);
			update.setForceUpdate(force || remote);
			Result result = update.delete();
			if (result == Result.REJECTED) {
				String err = "The branch '%s' is not an ancestor of your current HEAD.\n"
						+ "If you are sure you want to delete it, run 'jgit branch -D %1$s'.";
				throw die(String.format(err, branch));
			} else if (result == Result.NEW)
				throw die(String.format("branch '%s' not found.", branch));
			if (remote)
				out.println(String.format("Deleted remote branch %s", branch));
			else if (verbose)
				out.println(String.format("Deleted branch %s", branch));
		}
	}
}
