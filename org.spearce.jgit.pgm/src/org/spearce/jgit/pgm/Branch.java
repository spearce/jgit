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

import java.util.Map;
import java.util.TreeSet;

import org.kohsuke.args4j.Option;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.Ref;

@Command(common = true, usage = "List, create, or delete branches")
class Branch extends TextBuiltin {

	@Option(name = "--remote", aliases = { "-r" }, usage = "act on remote-tracking branches")
	boolean remote = false;

	@Option(name = "--all", aliases = { "-a" }, usage = "list both remote-tracking and local branches")
	boolean all = false;

	@Override
	protected void run() throws Exception {
		Map<String, Ref> refs = db.getAllRefs();
		Ref head = refs.get(Constants.HEAD);
		// This can happen if HEAD is stillborn
		if (head != null) {
			String current = head.getName();
			if (current.equals(Constants.HEAD))
				printHead("(no branch)", true);
			for (String ref : new TreeSet<String>(refs.keySet())) {
				if (isHead(ref))
					printHead(ref, current.equals(ref));
			}
		}
	}

	private boolean isHead(String key) {
		return (all || !remote) && key.startsWith(Constants.HEADS_PREFIX)
				|| (all || remote) && key.startsWith(Constants.REMOTES_PREFIX);
	}

	private void printHead(String ref, boolean isCurrent) {
		out.print(isCurrent ? '*' : ' ');
		out.print(' ');
		ref = ref.substring(ref.indexOf('/', 5) + 1);
		out.println(ref);
	}
}
