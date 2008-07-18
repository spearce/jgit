/*
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
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

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.PersonIdent;

class Tag extends TextBuiltin {
	@Override
	void execute(String[] args) throws Exception {
		String tagName = null;
		String message = null;
		String ref = "HEAD";
		boolean force = false;
               if (args.length == 0)
                       usage();
		for (int i = 0; i < args.length; ++i) {
			if (args[i].equals("-f")) {
				force = true;
				continue;
			}
			if (args[i].equals("-m")) {
				if (i < args.length - 2)
					message = args[i++] + "\n";
				else
					usage();
				continue;
			}
			if (args[i].startsWith("-m")) {
				message = args[i].substring(2) + "\n";
				continue;
			}
			if (args[i].startsWith("-") && i == args.length - 1)
				usage();
			if (i == args.length - 2) {
				tagName = args[i];
				ref = args[i+1];
				++i;
				continue;
			}
			if (i == args.length - 1) {
				tagName = args[i];
				continue;
			}
			usage();
		}
		if (!tagName.startsWith(Constants.TAGS_PREFIX + "/"))
			tagName = Constants.TAGS_PREFIX + "/" + tagName;
		if (!force && db.resolve(tagName) != null) {
			throw die("fatal: tag '"
					+ tagName.substring(Constants.TAGS_PREFIX.length() + 1)
					+ "' exists");
		}
		org.spearce.jgit.lib.Tag tag = new org.spearce.jgit.lib.Tag(db);
		tag.setObjId(db.resolve(ref));
		if (message != null) {
			message = message.replaceAll("\r", "");
			tag.setMessage(message);
			tag.setTagger(new PersonIdent(db));
			tag.setType("commit");
		}
		tag.setTag(tagName.substring(Constants.TAGS_PREFIX.length() + 1));
		tag.tag();
	}

	private void usage() {
               throw die("Usage: [-m message] [-f] tag [head]");
	}
}
